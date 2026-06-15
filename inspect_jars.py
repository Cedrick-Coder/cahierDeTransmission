import zipfile
from pathlib import Path
import re
base = Path(r'D:\cahierDeTransmission\front_end\android\app\libs')
print('Searching jars in', base)
for jar_name in ['zkandroidfpreader.jar', 'zkandroidcore.jar']:
    jar_path = base / jar_name
    print('===', jar_name)
    with zipfile.ZipFile(jar_path, 'r') as z:
        for entry in ['com/zkteco/android/biometric/module/fingerprintreader/FingerprintFactory.class',
                      'com/zkteco/android/biometric/module/fingerprintreader/FingerprintSensor.class',
                      'com/zkteco/android/biometric/core/device/TransportType.class',
                      'com/zkteco/android/biometric/core/device/BiometricFactory.class',
                      'com/zkteco/android/biometric/core/device/TransportDevice.class']:
            if entry not in z.namelist():
                print('MISSING', entry)
                continue
            print(entry)
            data = z.read(entry)
            interesting = []
            for s in re.findall(rb'[ -~]{4,}', data):
                if any(x in s for x in [b'Fingerprint', b'Factory', b'create', b'open', b'Open', b'USB', b'TransportType', b'USBHID', b'USBMCU', b'USBSCSI', b'Init', b'Close', b'Acquire']):
                    interesting.append(s.decode('utf-8', 'ignore'))
            for s in sorted(set(interesting)):
                print('   ', s)
