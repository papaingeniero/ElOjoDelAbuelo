
import os

path = 'app/src/main/java/com/elojodelabuelo/NanoHttpServer.java'
target_line_part = 'var durationSec = Math.round((Date.now() - gRecStartTime) / 1000);'
insertion = '                "function finalizeRecordingCard(filename) {\\n" +\n'

with open(path, 'r') as f:
    lines = f.readlines()

new_lines = []
found = False
for line in lines:
    if target_line_part in line and not found:
        # Check if previous line is already the function def (idempotency)
        if len(new_lines) > 0 and 'function finalizeRecordingCard' in new_lines[-1]:
             print("Already fixed?")
        else:
             print("Inserting missing header...")
             new_lines.append(insertion)
        found = True
    new_lines.append(line)

with open(path, 'w') as f:
    f.writelines(new_lines)

if found:
    print("Success: Line inserted.")
else:
    print("Error: Target line not found.")
