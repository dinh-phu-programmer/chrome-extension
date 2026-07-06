import sys
import json
import struct
import subprocess
import os

def read_message():
    raw_length = sys.stdin.buffer.read(4)
    if not raw_length:
        return None
    length = struct.unpack('@I', raw_length)[0]
    data = sys.stdin.buffer.read(length)
    return json.loads(data.decode('utf-8'))

def send_message(obj):
    encoded = json.dumps(obj).encode('utf-8')
    sys.stdout.buffer.write(struct.pack('@I', len(encoded)))
    sys.stdout.buffer.write(encoded)
    sys.stdout.buffer.flush()

def browse_folder():
    try:
        import tkinter as tk
        from tkinter import filedialog
        root = tk.Tk()
        root.withdraw()
        root.wm_attributes('-topmost', 1)
        folder = filedialog.askdirectory(title='Select Folder')
        root.destroy()
        if folder:
            send_message({'success': True, 'path': folder.replace('/', '\\')})
        else:
            send_message({'success': False, 'message': 'cancelled'})
    except Exception as e:
        send_message({'success': False, 'message': str(e)})

def open_folder(path):
    if not os.path.exists(path):
        send_message({'success': False, 'message': f'Path does not exist: {path}'})
        return
    try:
        subprocess.Popen(['explorer', os.path.normpath(path)])
        send_message({'success': True, 'message': f'Opened: {path}'})
    except Exception as e:
        send_message({'success': False, 'message': str(e)})

def main():
    msg = read_message()
    if not msg:
        send_message({'success': False, 'message': 'No message received'})
        return

    action = msg.get('action', 'open')

    if action == 'browse':
        browse_folder()
    elif action == 'open':
        path = msg.get('path', '').strip()
        if not path:
            send_message({'success': False, 'message': 'No path provided'})
            return
        open_folder(path)
    else:
        send_message({'success': False, 'message': f'Unknown action: {action}'})

if __name__ == '__main__':
    main()
