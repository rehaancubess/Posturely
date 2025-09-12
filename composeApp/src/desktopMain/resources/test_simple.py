#!/usr/bin/env python3
"""
Simple test script to verify Python subprocess communication
"""

import sys
import json
import time

def main():
    print("Simple Python test script started", file=sys.stderr)
    print("Ready to receive input", file=sys.stderr)
    
    # Ensure output is not buffered
    sys.stdout.reconfigure(line_buffering=True)
    
    try:
        for line in sys.stdin:
            try:
                print(f"Received: '{line.strip()}'", file=sys.stderr)
                
                # Echo back the input
                response = {
                    'type': 'echo',
                    'received': line.strip(),
                    'timestamp': time.time()
                }
                
                response_json = json.dumps(response)
                print(response_json)
                sys.stdout.flush()  # Ensure output is sent immediately
                print(f"Sent response: {response}", file=sys.stderr)
                
            except Exception as e:
                print(f"Error: {e}", file=sys.stderr)
                error_response = {
                    'type': 'error',
                    'message': str(e)
                }
                print(json.dumps(error_response))
                sys.stdout.flush()
                
    except KeyboardInterrupt:
        print("Interrupted", file=sys.stderr)
    except Exception as e:
        print(f"Fatal error: {e}", file=sys.stderr)

if __name__ == "__main__":
    main()
