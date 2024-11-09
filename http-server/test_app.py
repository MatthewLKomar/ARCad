import requests
import data_pb2
from time import sleep

GET_IP = "localhost"

while True:
    response = requests.get(f"http://{GET_IP}:5000/get_latest_data")
    if response.status_code == 200:
        # Deserialize the protobuf data from bytes
        data = data_pb2.Data()
        data.ParseFromString(response.content)
        print(f"ID: {data.id}, Value: {data.value}, Timestamp: {data.timestamp}, Options: {data.opts}")
        sleep(1)
    else:
        print("Failed to fetch data.")
        break
