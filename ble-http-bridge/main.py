import argparse
from threading import Thread
from web_server import WebServer
from queue import Queue

# Variable to hold the most recent data
latest_data = {
    "id": 0,
    "value": 0,
    "accel": {  # unused
    "x": 0.0,
    "y": 0.0,
    "z": 0.0
  },
  "gyro": { # unused
    "x": 0.0,
    "y": 0.0,
    "z": 0.0
  },
    "timestamp": 0.0,
    "opts": ""
}

def get_input(q):
    """Function to handle command-line input from the user."""
    global latest_data
    parser = argparse.ArgumentParser(description='Update Data fields', add_help=False)
    parser.add_argument('--value', type=int, help='Value for the data')
    parser.add_argument('--timestamp', type=float, help='Timestamp for the data')
    parser.add_argument('--opts', type=str, help='Optional string for the data')
    parser.add_argument('--help', '-h', action='store_true', help='Show help message')

    while True:
        args, _ = parser.parse_known_args(input().split())
        if args.value is not None:
            latest_data["value"] = args.value
        if args.timestamp is not None:
            latest_data["timestamp"] = args.timestamp
        if args.opts is not None:
            latest_data["opts"] = args.opts
        if args.help:
            parser.print_help()
        
        print(f"Updated data:\n{latest_data}")
        q.put(latest_data.copy())
        latest_data["id"] += 1

if __name__ == '__main__':

    # Start the Flask server
    q = Queue()
    ws = WebServer(q)

    # Start the input thread to accept user inputs
    input_thread = Thread(target=get_input, args=(q, ), daemon=True)
    input_thread.start()

    ws.start()
