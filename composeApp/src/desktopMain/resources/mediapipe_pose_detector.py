#!/usr/bin/env python3
"""
MediaPipe Pose Detection Service for Desktop App
This script runs as a subprocess and communicates with the Kotlin app via file-based notifications
"""

import sys
import json
import time
import base64
import numpy as np
import cv2
import io
from PIL import Image
import os
import signal
import glob
import threading
from pathlib import Path

# Communication directories
COMMAND_DIR = "/tmp/posture_commands"
RESPONSE_DIR = "/tmp/posture_responses"
PID_FILE = "/tmp/posture_python.pid"

# Try to import MediaPipe, but handle gracefully if not available
try:
    import mediapipe as mp
    from mediapipe.tasks import python
    from mediapipe.tasks.python import vision
    MEDIAPIPE_AVAILABLE = True
except ImportError:
    MEDIAPIPE_AVAILABLE = False
    print("Warning: MediaPipe not available. Install with: pip install mediapipe", file=sys.stderr)

class MediaPipePoseDetector:
    def __init__(self):
        self.landmarker = None
        self.is_initialized = False
        if MEDIAPIPE_AVAILABLE:
            self.running_mode = vision.RunningMode.VIDEO
        else:
            self.running_mode = None
        
    def initialize(self, model_path=None):
        """Initialize MediaPipe Pose Landmarker"""
        try:
            # Check if MediaPipe is available
            if not MEDIAPIPE_AVAILABLE:
                print("Error: MediaPipe is not available", file=sys.stderr)
                return False
            
            # Use default model if none provided
            if model_path is None:
                model_path = "pose_landmarker_full.task"
            
            print(f"Initializing MediaPipe Pose with model: {model_path}", file=sys.stderr)
            
            # Check if model file exists
            if not os.path.exists(model_path):
                print(f"Error: Model file {model_path} not found", file=sys.stderr)
                return False
            
            # Check if model file is readable and has content
            try:
                if os.path.getsize(model_path) == 0:
                    print(f"Error: Model file {model_path} is empty", file=sys.stderr)
                    return False
            except OSError as e:
                print(f"Error checking model file: {str(e)}", file=sys.stderr)
                return False
            
            print(f"Model file found: {os.path.abspath(model_path)}", file=sys.stderr)
            
            # Create pose landmarker options
            base_options = python.BaseOptions(model_asset_path=model_path)
            options = vision.PoseLandmarkerOptions(
                base_options=base_options,
                running_mode=vision.RunningMode.VIDEO,
                num_poses=1,
                min_pose_detection_confidence=0.5,
                min_pose_presence_confidence=0.5,
                min_tracking_confidence=0.5,
                output_segmentation_masks=False
            )
            
            print("Creating PoseLandmarker...", file=sys.stderr)
            
            # Create the landmarker
            try:
                self.landmarker = vision.PoseLandmarker.create_from_options(options)
                self.is_initialized = True
                print("MediaPipe Pose initialized successfully", file=sys.stderr)
                return True
            except Exception as e:
                print(f"Error creating PoseLandmarker: {str(e)}", file=sys.stderr)
                import traceback
                traceback.print_exc(file=sys.stderr)
                self.is_initialized = False
                return False
                
        except ImportError as e:
            print(f"Import error - MediaPipe not available: {str(e)}", file=sys.stderr)
            print("Please ensure MediaPipe is installed: pip install mediapipe", file=sys.stderr)
            self.is_initialized = False
            return False
        except FileNotFoundError as e:
            print(f"Model file not found: {str(e)}", file=sys.stderr)
            self.is_initialized = False
            return False
        except Exception as e:
            print(f"Error initializing MediaPipe: {str(e)}", file=sys.stderr)
            import traceback
            traceback.print_exc(file=sys.stderr)
            self.is_initialized = False
            return False
    
    def process_frame(self, frame_data, timestamp_ms):
        """Process a frame and return pose landmarks"""
        if not MEDIAPIPE_AVAILABLE:
            return {
                'landmarks': [],
                'timestamp': timestamp_ms,
                'success': False,
                'error': 'MediaPipe not available'
            }
        
        if not self.is_initialized or self.landmarker is None:
            return None
        
        try:
            # Convert base64 frame data to numpy array
            frame_bytes = base64.b64decode(frame_data)
            frame_array = np.frombuffer(frame_bytes, dtype=np.uint8)
            frame = cv2.imdecode(frame_array, cv2.IMREAD_COLOR)
            
            if frame is None:
                print("Error: Failed to decode frame data", file=sys.stderr)
                return None
            
            # Convert BGR to RGB
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            # Convert to MediaPipe Image
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=frame_rgb)
            
            # Detect pose landmarks
            result = self.landmarker.detect(mp_image)
            
            if result.pose_landmarks:
                # Extract landmarks from the first detected pose
                landmarks = result.pose_landmarks[0]
                
                # Convert landmarks to our format
                landmark_data = []
                for landmark in landmarks:
                    landmark_data.append({
                        'x': landmark.x,
                        'y': landmark.y,
                        'z': landmark.z,
                        'visibility': landmark.visibility,
                        'presence': landmark.presence
                    })
                
                return {
                    'landmarks': landmark_data,
                    'timestamp': timestamp_ms,
                    'success': True
                }
            else:
                return {
                    'landmarks': [],
                    'timestamp': timestamp_ms,
                    'success': False,
                    'message': 'No pose detected'
                }
                
        except ValueError as e:
            print(f"Value error processing frame: {str(e)}", file=sys.stderr)
            return {
                'landmarks': [],
                'timestamp': timestamp_ms,
                'success': False,
                'error': f"Invalid frame data: {str(e)}"
            }
        except Exception as e:
            print(f"Error processing frame: {str(e)}", file=sys.stderr)
            import traceback
            traceback.print_exc(file=sys.stderr)
            return {
                'landmarks': [],
                'timestamp': timestamp_ms,
                'success': False,
                'error': str(e)
            }
    
    def close(self):
        """Clean up resources"""
        if MEDIAPIPE_AVAILABLE and self.landmarker:
            try:
                self.landmarker.close()
            except Exception as e:
                print(f"Error closing landmarker: {str(e)}", file=sys.stderr)
        self.is_initialized = False

def setup_communication_dirs():
    """Create communication directories if they don't exist"""
    os.makedirs(COMMAND_DIR, exist_ok=True)
    os.makedirs(RESPONSE_DIR, exist_ok=True)
    
    # Write PID file for Kotlin to track
    with open(PID_FILE, 'w') as f:
        f.write(str(os.getpid()))
    
    print(f"Communication directories created: {COMMAND_DIR}, {RESPONSE_DIR}", file=sys.stderr)
    print(f"PID file written: {PID_FILE}", file=sys.stderr)

def cleanup_communication_dirs():
    """Clean up communication files"""
    try:
        # Remove PID file
        if os.path.exists(PID_FILE):
            os.remove(PID_FILE)
        
        # Clean up command files
        for file in glob.glob(f"{COMMAND_DIR}/*.json"):
            os.remove(file)
        
        # Clean up response files
        for file in glob.glob(f"{RESPONSE_DIR}/*.json"):
            os.remove(file)
            
        print("Communication directories cleaned up", file=sys.stderr)
    except Exception as e:
        print(f"Error during cleanup: {str(e)}", file=sys.stderr)

def send_response(response_type, data, request_id):
    """Send a response to the Kotlin app"""
    try:
        response = {
            'type': response_type,
            'request_id': request_id,
            'timestamp': int(time.time() * 1000),
            'data': data
        }
        
        response_file = f"{RESPONSE_DIR}/response_{request_id}.json"
        with open(response_file, 'w') as f:
            json.dump(response, f)
        
        print(f"Response sent: {response_type} -> {response_file}", file=sys.stderr)
        
    except Exception as e:
        print(f"Error sending response: {str(e)}", file=sys.stderr)

def process_command(command_data, request_id):
    """Process a command from the Kotlin app"""
    try:
        cmd_type = command_data.get('type')
        
        if not cmd_type:
            send_response('error', {'message': 'Command missing type field'}, request_id)
            return
        
        print(f"Processing command: {cmd_type}", file=sys.stderr)
        
        if cmd_type == 'init':
            # Initialize MediaPipe
            model_path = command_data.get('model_path')
            print(f"Initializing with model: {model_path}", file=sys.stderr)
            
            if not model_path:
                send_response('init_response', {
                    'success': False,
                    'message': 'No model path provided'
                }, request_id)
                return
            
            if not isinstance(model_path, str):
                send_response('init_response', {
                    'success': False,
                    'message': f'Invalid model path type: {type(model_path)}'
                }, request_id)
                return
            
            success = detector.initialize(model_path)
            send_response('init_response', {
                'success': success,
                'message': 'Initialized successfully' if success else 'Initialization failed'
            }, request_id)
            
        elif cmd_type == 'detect':
            # Process frame
            frame_data = command_data.get('frame_data')
            raw_timestamp = command_data.get('timestamp')
            
            # Validate and parse timestamp
            try:
                if raw_timestamp:
                    timestamp = int(raw_timestamp)
                else:
                    timestamp = int(time.time() * 1000)
            except (ValueError, TypeError):
                timestamp = int(time.time() * 1000)
                print(f"Warning: Invalid timestamp '{raw_timestamp}', using current time", file=sys.stderr)
            
            # Check if frame data is valid
            if not frame_data:
                send_response('detection_result', {
                    'landmarks': [],
                    'timestamp': timestamp,
                    'success': False,
                    'message': 'No frame data provided'
                }, request_id)
                return
            
            if not isinstance(frame_data, str):
                send_response('detection_result', {
                    'landmarks': [],
                    'timestamp': timestamp,
                    'success': False,
                    'message': f'Invalid frame data type: {type(frame_data)}'
                }, request_id)
                return
            
            if not MEDIAPIPE_AVAILABLE:
                # Return mock data when MediaPipe is not available
                send_response('detection_result', {
                    'landmarks': [],
                    'timestamp': timestamp,
                    'success': False,
                    'message': 'MediaPipe not available - using mock data'
                }, request_id)
                return
            
            result = detector.process_frame(frame_data, timestamp)
            send_response('detection_result', result, request_id)
            
        elif cmd_type == 'ping':
            # Heartbeat/ping command
            send_response('pong', {
                'alive': True,
                'mediapipe_available': MEDIAPIPE_AVAILABLE,
                'initialized': detector.is_initialized
            }, request_id)
            
        elif cmd_type == 'status':
            # Status command to check service health
            send_response('status_response', {
                'mediapipe_available': MEDIAPIPE_AVAILABLE,
                'initialized': detector.is_initialized
            }, request_id)
            
        elif cmd_type == 'close':
            # Close service
            detector.close()
            send_response('close_response', {'success': True}, request_id)
            
        else:
            send_response('error', {
                'message': f'Unknown command type: {cmd_type}'
            }, request_id)
            
    except Exception as e:
        print(f"Error processing command: {str(e)}", file=sys.stderr)
        import traceback
        traceback.print_exc(file=sys.stderr)
        send_response('error', {
            'message': f'Processing error: {str(e)}'
        }, request_id)

def watch_for_commands():
    """Watch for command files and process them"""
    print("Starting command watcher...", file=sys.stderr)
    
    while True:
        try:
            # Look for command files
            command_files = glob.glob(f"{COMMAND_DIR}/*.json")
            
            for command_file in command_files:
                try:
                    # Read command
                    with open(command_file, 'r') as f:
                        command_data = json.load(f)
                    
                    # Extract request ID from filename
                    request_id = os.path.basename(command_file).replace('.json', '')
                    
                    print(f"Processing command file: {command_file}", file=sys.stderr)
                    
                    # Process command
                    process_command(command_data, request_id)
                    
                    # Remove command file
                    os.remove(command_file)
                    
                except Exception as e:
                    print(f"Error processing command file {command_file}: {str(e)}", file=sys.stderr)
                    # Try to remove the problematic file
                    try:
                        os.remove(command_file)
                    except:
                        pass
            
            # Small delay to prevent excessive CPU usage
            time.sleep(0.1)
            
        except KeyboardInterrupt:
            print("Command watcher interrupted", file=sys.stderr)
            break
        except Exception as e:
            print(f"Error in command watcher: {str(e)}", file=sys.stderr)
            time.sleep(1)  # Wait before retrying

def main():
    """Main function to handle communication with Kotlin app"""
    global detector
    detector = MediaPipePoseDetector()
    
    print("MediaPipe Pose Detector Service Started", file=sys.stderr)
    
    # Set up signal handlers for graceful shutdown
    def signal_handler(signum, frame):
        print("Received signal, shutting down gracefully...", file=sys.stderr)
        detector.close()
        cleanup_communication_dirs()
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Set up communication directories
    setup_communication_dirs()
    
    print("Ready to receive commands via file notifications", file=sys.stderr)
    print("Watching for commands in:", COMMAND_DIR, file=sys.stderr)
    
    try:
        # Start watching for commands
        watch_for_commands()
        
    except KeyboardInterrupt:
        print("Service interrupted by user", file=sys.stderr)
    except MemoryError as e:
        print(f"Memory error: {str(e)}", file=sys.stderr)
        print("Consider reducing frame size or model complexity", file=sys.stderr)
    except Exception as e:
        print(f"Fatal error: {str(e)}", file=sys.stderr)
        import traceback
        traceback.print_exc(file=sys.stderr)
    finally:
        try:
            detector.close()
            cleanup_communication_dirs()
            print("Service stopped", file=sys.stderr)
        except Exception as e:
            print(f"Error during cleanup: {str(e)}", file=sys.stderr)

if __name__ == "__main__":
    main()
