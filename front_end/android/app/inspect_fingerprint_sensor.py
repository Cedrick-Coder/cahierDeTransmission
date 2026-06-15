import zipfile
from io import BytesIO
import os

jar_path = os.path.join('libs', 'zkandroidfpreader.jar')
class_path = 'com/zkteco/android/biometric/module/fingerprintreader/FingerprintSensor.class'
with zipfile.ZipFile(jar_path, 'r') as z:
    data = z.read(class_path)

f = BytesIO(data)
def read_u1():
    return int.from_bytes(f.read(1), 'big')
def read_u2():
    return int.from_bytes(f.read(2), 'big')
def read_u4():
    return int.from_bytes(f.read(4), 'big')

f.read(4)
f.read(2)
f.read(2)
cp_count = read_u2()
print('cp_count', cp_count)
cp = [None] * cp_count
idx = 1
while idx < cp_count:
    tag = read_u1()
    if tag == 1:
        length = read_u2()
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

f.read(2)
f.read(2)
f.read(2)
interfaces_count = read_u2()
print('interfaces_count', interfaces_count)
for _ in range(interfaces_count):
    f.read(2)
fields_count = read_u2()
print('fields_count', fields_count)
for _ in range(fields_count):
    f.read(2)
    f.read(2)
    f.read(2)
    attr_count = read_u2()
    for __ in range(attr_count):
        f.read(2)
        f.read(read_u4())
methods_count = read_u2()
method_names = []
for _ in range(methods_count):
    f.read(2)
    name_index = read_u2()
    desc_index = read_u2()
    name = cp[name_index]
    desc = cp[desc_index]
    method_names.append((name, desc))
    attr_count = read_u2()
    for __ in range(attr_count):
        f.read(2)
        f.read(read_u4())

for name, desc in sorted(method_names):
    print(f'{name} {desc}')
