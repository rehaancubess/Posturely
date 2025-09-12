#!/usr/bin/env python3
"""
Local WebSocket Pose Server (MediaPipe) for Desktop KMP

Protocol (JSON over WebSocket):
- Client -> Server:
  {"type":"init"}
  {"type":"detect","image":"<base64 image>","ts":<ms>}
  {"type":"ping"}
  {"type":"close"}

- Server -> Client:
  {"type":"init_response","success":true}
  {"type":"detection","success":true,"landmarks":[{x,y,z,visibility,presence}],"timestamp":<ms>}
  {"type":"pong","alive":true,"mediapipe_available":true,"initialized":<bool>}
  {"type":"error","message":"..."}
"""

import asyncio
import base64
import json
import os
import sys
import traceback
from typing import Optional

import numpy as np
import cv2

try:
    import mediapipe as mp
    from mediapipe import solutions as mp_solutions
    MEDIAPIPE_AVAILABLE = True
except Exception as e:
    MEDIAPIPE_AVAILABLE = False
    print(f"MediaPipe import failed: {e}", file=sys.stderr)

try:
    import websockets
    from websockets.server import WebSocketServerProtocol
except Exception as e:
    print("Please install websockets: pip install websockets", file=sys.stderr)
    raise


class PoseSession:
    def __init__(self):
        self.pose: Optional[object] = None
        self.initialized: bool = False

    def init_pose(self) -> bool:
        if not MEDIAPIPE_AVAILABLE:
            return False
        try:
            mp_pose = mp_solutions.pose
            # Tune for real-time similar to your working script
            self.pose = mp_pose.Pose(
                min_detection_confidence=0.5,
                min_tracking_confidence=0.5,
                model_complexity=1,
                smooth_landmarks=True,
                enable_segmentation=False,
            )
            self.initialized = True
            return True
        except Exception as e:
            print(f"Pose init error: {e}", file=sys.stderr)
            traceback.print_exc(file=sys.stderr)
            self.initialized = False
            return False

    def close(self):
        try:
            if self.pose is not None:
                self.pose.close()
        except Exception:
            pass
        self.pose = None
        self.initialized = False

    def detect(self, b64_image: str, timestamp: int):
        if not self.initialized or self.pose is None:
            return {"success": False, "timestamp": timestamp, "message": "not_initialized"}
        try:
            img_bytes = base64.b64decode(b64_image)
            arr = np.frombuffer(img_bytes, dtype=np.uint8)
            frame = cv2.imdecode(arr, cv2.IMREAD_COLOR)
            if frame is None:
                return {"success": False, "timestamp": timestamp, "message": "decode_failed"}

            # BGR -> RGB
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            rgb.flags.writeable = False
            results = self.pose.process(rgb)
            rgb.flags.writeable = True

            if results.pose_landmarks and hasattr(results.pose_landmarks, 'landmark'):
                landmarks = []
                for lm in results.pose_landmarks.landmark:
                    landmarks.append({
                        "x": float(lm.x),
                        "y": float(lm.y),
                        "z": float(lm.z),
                        "visibility": float(getattr(lm, 'visibility', 0.0)),
                        "presence": float(getattr(lm, 'presence', 0.0)),
                    })
                return {"success": True, "timestamp": timestamp, "landmarks": landmarks}
            else:
                return {"success": False, "timestamp": timestamp, "message": "no_pose"}
        except Exception as e:
            print(f"Detect error: {e}", file=sys.stderr)
            traceback.print_exc(file=sys.stderr)
            return {"success": False, "timestamp": timestamp, "message": str(e)}


async def handler(ws: WebSocketServerProtocol):
    session = PoseSession()
    try:
        async for message in ws:
            try:
                data = json.loads(message)
            except Exception:
                await ws.send(json.dumps({"type": "error", "message": "invalid_json"}))
                continue

            mtype = data.get("type")
            if mtype == "init":
                ok = session.init_pose()
                await ws.send(json.dumps({"type": "init_response", "success": bool(ok)}))
            elif mtype == "detect":
                b64img = data.get("image")
                ts = int(data.get("ts") or 0)
                if not isinstance(b64img, str):
                    await ws.send(json.dumps({"type": "detection", "success": False, "timestamp": ts, "message": "no_image"}))
                    continue
                result = session.detect(b64img, ts)
                result["type"] = "detection"
                await ws.send(json.dumps(result))
            elif mtype == "ping":
                await ws.send(json.dumps({
                    "type": "pong",
                    "alive": True,
                    "mediapipe_available": MEDIAPIPE_AVAILABLE,
                    "initialized": session.initialized,
                }))
            elif mtype == "close":
                await ws.send(json.dumps({"type": "close_response", "success": True}))
                break
            else:
                await ws.send(json.dumps({"type": "error", "message": f"unknown_type:{mtype}"}))
    finally:
        session.close()


async def main():
    host = "127.0.0.1"
    port = int(os.environ.get("POSE_WS_PORT", "8765"))
    print(f"Starting WS Pose server at ws://{host}:{port}", file=sys.stderr)
    async with websockets.serve(handler, host, port, max_size=8 * 1024 * 1024):
        await asyncio.Future()  # run forever


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass

