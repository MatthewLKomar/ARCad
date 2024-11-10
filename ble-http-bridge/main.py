import asyncio
from multiprocessing import Queue, Process

from argparse import ArgumentParser
from web_server import WebServer
from bluetoothclient import BluetoothClient

from time import sleep, time

# async helpers

background_tasks = set()

def add_to_background_tasks(coroutine, *args, **kwargs):
    task = asyncio.create_task(coroutine(*args, **kwargs))
    background_tasks.add(task)
    task.add_done_callback(background_tasks.discard)
    return task

async def dloop(q, args):
    await BluetoothClient(q, args.name, args).run()

# processes

async def ble_main(q, args):
    await add_to_background_tasks(dloop, q, args)

def ble(q, args):
    asyncio.run(ble_main(q, args))

def mock_ble(q, args):
    idx = 0
    strs = [
        "hello",
        "message",
        "goodbye",
        "done"
    ]
    while True:
        curr_str = strs[idx % len(strs)]
        idx += 1
        print("sending: ", curr_str)
        q.put({"id": 0, "value": 1, "timestamp": time(), "opts": curr_str})
        sleep(1)

def web(q, args):
    ws = WebServer(q)
    ws.start()

# runner

def create_parser():
    parser = ArgumentParser(description='Connect to a BLE device')
    parser.add_argument('--name', dest='name')
    parser.add_argument('--no-ble', dest='no_ble', action='store_true')
    parser.add_argument('--timeout', '-t', dest='timeout', default=20.0)
    parser.add_argument('--gatt_char', '-g', dest='gatt_descriptor', default='Nordic UART TX')
    parser.add_argument('--config_file', '-c', dest='config_file')
    return parser

def main():
    parser = create_parser()
    args = parser.parse_args()

    q = Queue()

    if args.no_ble:
        p1 = Process(target=mock_ble, args=(q, args))
    else:
        p1 = Process(target=ble, args=(q, args))
    p1.start()

    p2 = Process(target=web, args=(q, args))
    p2.start()


if __name__ == '__main__':
    main()

    