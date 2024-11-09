import argparse
import time
from flask import Flask, jsonify
import data_pb2  # The generated protobuf classes
from threading import Thread

app = Flask(__name__)

# Variable to hold the most recent data
latest_data = data_pb2.Data()
curr_id = 0

# Function to simulate periodic updates of data (in this case, updated via user input)
def update_data():
    global latest_data
    while True:
        # Wait for user input in the main thread to update the protobuf object
        time.sleep(1)  # Simulate periodic update to give the user time to input data

def get_input():
    """Function to handle command-line input from the user."""
    global latest_data
    global curr_id
    parser = argparse.ArgumentParser(description='Update Data fields', add_help=False)
    parser.add_argument('--value', type=int, help='Value for the data')
    parser.add_argument('--timestamp', type=float, help='Timestamp for the data')
    parser.add_argument('--opts', type=str, help='Optional string for the data')
    parser.add_argument('--help', '-h', action='store_true', help='Show help message')

    while True:
        args, _ = parser.parse_known_args(input().split())
        if args.value is not None:
            latest_data.value = args.value
        if args.timestamp is not None:
            latest_data.timestamp = args.timestamp
        if args.opts is not None:
            latest_data.opts = args.opts
        if args.help:
            parser.print_help()
        
        print(f"Updated data:\n{latest_data}")
        curr_id += 1

@app.route('/get_latest_data', methods=['GET'])
def get_latest_data():
    """Endpoint to return the most recent protobuf data as a response."""
    if latest_data is None:
        return jsonify({"error": "No data available"}), 404
    
    # Return serialized protobuf data as bytes
    data_bytes = latest_data.SerializeToString()
    return data_bytes, 200

if __name__ == '__main__':
    # Start the input thread to accept user inputs
    input_thread = Thread(target=get_input, daemon=True)
    input_thread.start()

    # Start the Flask server
    print("Starting server...")
    app.run(host='0.0.0.0', port=5000)
