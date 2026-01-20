
import os

path = 'app/src/main/java/com/elojodelabuelo/NanoHttpServer.java'

with open(path, 'r') as f:
    text = f.read()

# Target the cleanupLivePreview function
target = 'function cleanupLivePreview() {'
end_target = 'setTimeout(function() { finalizeRecordingCard(gCurrentRecFilename); }, 1500);'

# New logic: 
# 1. Clear intervals/remove elements.
# 2. Refetch metatada to get the REAL filename (renamed with FPS).
# 3. Call finalize with the NEW filename.
new_code = r'''function cleanupLivePreview() {
   if(parasiteInterval) clearInterval(parasiteInterval);
   var img = document.getElementById('hidden-stream-source'); if(img) document.body.removeChild(img);
   setTimeout(function() { 
       fetch('/api/latest_video_meta').then(r=>r.json()).then(meta => {
            if(meta.filename) {
                finalizeRecordingCard(meta.filename); 
            } else {
                console.error("No filename found after finalization");
                location.reload();
            }
       }).catch(e => location.reload());
   }, 1500);
}'''

# Convert to Java String format
def to_java_string(js_code):
    lines = js_code.split('\n')
    java_str = ''
    for line in lines:
        escaped_line = line.replace('"', '\\"').replace("'", "'")
        java_str += '                "' + escaped_line + '\\n" +\n'
    return java_str

if target in text:
    print("Found cleanupLivePreview")
    # Finding start index
    lines = text.split('\n')
    start_idx = -1
    end_idx = -1
    
    for i, line in enumerate(lines):
        if 'function cleanupLivePreview() {' in line:
            start_idx = i
        if 'setTimeout(function() { finalizeRecordingCard(gCurrentRecFilename); }, 1500);' in line and start_idx != -1:
            end_idx = i
            break
            
    if start_idx != -1 and end_idx != -1:
         # end_idx + 1 usually has '}'
         end_block_idx = end_idx + 1
         
         # Construct replacement
         java_replacement = to_java_string(new_code).strip()
         if not java_replacement.endswith('+'):
            java_replacement += ' +'
            
         # Check if next line is not script end
         # Actually just replacing the whole function block lines
         new_lines = lines[:start_idx] + [java_replacement] + lines[end_block_idx+1:]
         
         with open(path, 'w') as f:
             f.write('\n'.join(new_lines))
         print("Success")
    else:
        print("Could not align lines")

else:
    print("Function not found")
