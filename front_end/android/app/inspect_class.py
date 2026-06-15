import zipfile
from io import BytesIO
from pathlib import Path
import sys


def read_u1(f):
    return int.from_bytes(f.read(1), 'big')

def read_u2(f):
    return int.from_bytes(f.read(2), 'big')

def read_u4(f):
    return int.from_bytes(f.read(4), 'big')


def parse_class(blob):
    f = BytesIO(blob)
    f.read(4)  # magic
    f.read(2)  # minor
    f.read(2)  # major
    cp_count = read_u2(f)
    cp = [None] * cp_count
    idx = 1
    while idx < cp_count:
        tag = read_u1(f)
        if tag == 1:
            length = read_u2(f)
            cp[idx] = f.read(length).decode('utf-8', 'replace')
        elif tag in (3, 4):
            f.read(4)
        elif tag in (5, 6):
            f.read(8)
            idx += 1
        elif tag in (7, 8):
            f.read(2)
        elif tag in (9, 10, 11, 12, 18):
            f.read(4)
        elif tag == 15:
            f.read(3)
        elif tag == 16:
            f.read(2)
        else:
            raise ValueError(f'Unknown tag {tag}')
        idx += 1
    f.read(2); f.read(2); f.read(2)
    interfaces_count = read_u2(f)
    for _ in range(interfaces_count): f.read(2)
    fields_count = read_u2(f)
    for _ in range(fields_count):
        f.read(2); f.read(2); f.read(2)
        attr_count = read_u2(f)
        for __ in range(attr_count): f.read(2); f.read(read_u4(f))
    methods_count = read_u2(f)
    methods = []
    for _ in range(methods_count):
        f.read(2)
        name_index = read_u2(f)
        desc_index = read_u2(f)
        name = cp[name_index]
        desc = cp[desc_index]
        methods.append((name, desc))
        attr_count = read_u2(f)
        for __ in range(attr_count): f.read(2); f.read(read_u4(f))
    return methods


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('Usage: inspect_class.py <jar> <class_path>')
        sys.exit(1)
    jar_path = Path(sys.argv[1])
    class_path = sys.argv[2]
    with zipfile.ZipFile(jar_path, 'r') as z:
        blob = z.read(class_path)
    methods = parse_class(blob)
    for name, desc in sorted(methods):
        print(f'{name} {desc}')
