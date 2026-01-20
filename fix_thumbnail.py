
import os
import re

path = 'app/src/main/java/com/elojodelabuelo/NanoHttpServer.java'

with open(path, 'r') as f:
    text = f.read()

# Target the finalizeRecordingCard function
target_start = 'function finalizeRecordingCard(filename) {'
target_end_line = 'if(typeof gWebZoom !== \'undefined\') updateWebTransform(gWebZoom, gWebPanX, gWebPanY);'

new_code = r'''function finalizeRecordingCard(filename) {
    var durationSec = Math.round((Date.now() - gRecStartTime) / 1000);
    var durationStr = durationSec + "s";
    var videoUrl = '/' + filename;
    fetch(videoUrl, { method: 'HEAD' })
    .then(function(response) {
        var bytes = response.headers.get('content-length');
        var sizeMB = (bytes / (1024*1024)).toFixed(1) + " MB";
        var card = document.getElementById('temp-preview-card');
        if(!card) return;
        if(parasiteInterval) clearInterval(parasiteInterval);
        var dt = parseDateFromFilename(filename);
        var dateStr = dt.date; var timeStr = dt.time;
        
        // Derive preview filename from video filename (remove FPS suffix)
        // video_YYYYMMDD_HHMMSS_XXfps.mjpeg -> preview_YYYYMMDD_HHMMSS.mjpeg
        var previewFilename = filename.replace("video_", "preview_").replace(/_\\d+fps/, "");
        
        card.className = 'video-item';
        card.id = '';
        card.style = '';
        card.setAttribute('onclick', "playVideo('" + filename + "')");
        
        // Injected canvas for animation
        var innerContent = "<div class='thumb-container'>" +
            "<img class='thumb' src='/thumbnails/" + filename + "?t=" + Date.now() + "'>" +
            "<canvas class='mini-canvas' data-src='/" + previewFilename + "'></canvas>" + 
            "</div>" +
            "<div class='info'>" +
            "<div style='font-size:15px; font-weight:bold; color:#ffffff; margin-bottom:4px;'>" + dateStr + " &nbsp; " + timeStr + "</div>" +
            "<div style='color:#ccc; font-size:13px;'>" +
            "<b>💾 " + sizeMB + "</b>" +
            " &nbsp;|&nbsp; " +
            "<b>⏳ " + durationStr + "</b>" +
            " &nbsp;|&nbsp; " +
            "🎥 MJPEG" +
            "</div></div>";
        card.innerHTML = innerContent;
        if(typeof gWebZoom !== 'undefined') updateWebTransform(gWebZoom, gWebPanX, gWebPanY);
        
        // Start animation immediately
        var canvas = card.querySelector('.mini-canvas');
        if(canvas) loadMiniPreview('/' + previewFilename, canvas);
        
    }).catch(function(err) {
        console.error("Error finalizing card:", err);
        location.reload();
    });
}'''

def to_java_string(js_code):
    lines = js_code.split('\n')
    java_str = ''
    for line in lines:
        escaped_line = line.replace('"', '\\"').replace("'", "'")
        java_str += '                "' + escaped_line + '\\n" +\n'
    return java_str

# Careful replacement logic to avoid regex issues with Java string concatenation
lines = text.split('\n')
start_idx = -1
end_idx = -1

for i, line in enumerate(lines):
    if target_start in line:
        start_idx = i
    if target_end_line in line and start_idx != -1:
        # We need to find the specific closing brace for the fetch.then block
        # But replacing the whole function is safer if we find the boundaries.
        # Actually, let's target the inner content of the function mostly.
        pass

# Robust approach: Find start of function, find end of function (first line starting with 'function cleanupLivePreview')
for i, line in enumerate(lines):
    if 'function finalizeRecordingCard(filename) {' in line:
        start_idx = i
    if 'function cleanupLivePreview() {' in line and start_idx != -1:
        end_idx = i - 1 # Allow for some empty lines before cleanup
        break

if start_idx != -1 and end_idx != -1:
    print(f"Replacing lines {start_idx} to {end_idx}")
    java_replacement = to_java_string(new_code).strip()
    if not java_replacement.endswith('+'):
        java_replacement += ' +'
    
    # We remove the trailing '+' from the last line if it exists to match context, 
    # but here we are in a concatenation block likely.
    # The previous code ended with '}' + \n
    
    new_lines = lines[:start_idx] + [java_replacement] + lines[end_idx:]
    
    # Clean up potentially double empty lines or detached strings
    # But wait, lines[end_idx] is likely empty or just whitespace. 
    # We need to make sure we don't break the Java string chain if it continues.
    # Actually, finalizeRecordingCard is followed by cleanupLivePreview in the same string block.
    
    with open(path, 'w') as f:
        f.write('\n'.join(new_lines))
    print("Success")
else:
    print("Could not find function boundaries")
