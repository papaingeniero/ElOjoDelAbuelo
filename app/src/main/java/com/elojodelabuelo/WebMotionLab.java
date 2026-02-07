package com.elojodelabuelo;

import java.util.Locale;

public class WebMotionLab {

    public static String getHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>🔬 Laboratorio Forense</title>\n" +
                "    <style>\n" +
                "        body { background: #0d1117; color: #c9d1d9; font-family: monospace; margin: 0; padding: 0; height: 100vh; overflow: hidden; display: flex; }\n" +
                "        .sidebar { width: 300px; background: #161b22; border-right: 1px solid #30363d; display: flex; flex-direction: column; height: 100%; }\n" +
                "        .sidebar-header { padding: 15px; border-bottom: 1px solid #30363d; font-weight: bold; color: #58a6ff; display: flex; justify-content: space-between; align-items: center; }\n" +
                "        .refresh-btn { background: none; border: none; color: #8b949e; cursor: pointer; font-size: 16px; }\n" +
                "        .refresh-btn:hover { color: #58a6ff; }\n" +
                "        .video-list { flex: 1; overflow-y: auto; overflow-x: hidden; }\n" +
                "        .video-card { padding: 10px; border-bottom: 1px solid #21262d; cursor: pointer; transition: 0.2s; display: flex; gap: 10px; align-items: center; }\n" +
                "        .video-card:hover { background: #21262d; }\n" +
                "        .video-card.active { background: #1f6feb; color: white; border-left: 3px solid #58a6ff; }\n" +
                "        .v-thumb { width: 60px; height: 40px; background: #000; object-fit: cover; border-radius: 4px; }\n" +
                "        .v-info { display: flex; flex-direction: column; overflow: hidden; }\n" +
                "        .v-name { font-weight: bold; font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n" +
                "        .v-meta { font-size: 10px; opacity: 0.7; }\n" +
                "        \n" +
                "        .main-content { flex: 1; padding: 20px; display: flex; flex-direction: column; align-items: center; overflow-y: auto; }\n" +
                "        .container { width: 100%; max-width: 800px; }\n" +
                "        h1 { border-bottom: 1px solid #30363d; padding-bottom: 10px; color: #58a6ff; margin-top: 0; }\n" +
                "        .drop-zone { border: 2px dashed #30363d; border-radius: 6px; padding: 40px; text-align: center; color: #8b949e; cursor: pointer; transition: 0.2s; margin-bottom: 20px; }\n" +
                "        .drop-zone:hover { border-color: #58a6ff; background: #161b22; color: #58a6ff; }\n" +
                "        .lab-bench { display: none; width: 100%; }\n" +
                "        .viewport { position: relative; margin: 0 auto; border: 1px solid #30363d; display: inline-block; }\n" +
                "        video { display: block; max-width: 100%; }\n" +
                "        canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; mix-blend-mode: screen; }\n" +
                "        .controls { background: #161b22; padding: 15px; border-radius: 6px; margin-top: 20px; border: 1px solid #30363d; }\n" +
                "        .control-row { display: flex; justify-content: space-between; margin-bottom: 10px; align-items: center; }\n" +
                "        input[type=range] { width: 60%; accent-color: #58a6ff; }\n" +
                "        .val-disp { color: #58a6ff; font-weight: bold; width: 50px; text-align: right; }\n" +
                "        .graph-box { height: 100px; background: #000; border: 1px solid #30363d; margin-top: 10px; position: relative; overflow: hidden; }\n" +
                "        .graph-line { stroke: #238636; stroke-width: 1; fill: none; }\n" +
                "        .graph-threshold { stroke: #d29922; stroke-dasharray: 4; stroke-width: 1; }\n" +
                "        .graph-alert { stroke: #da3633; stroke-width: 2; opacity: 0.5; }\n" +
                "        .btn { background: #238636; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; font-family: inherit; margin-top: 10px; width: 100%; }\n" +
                "        .btn:hover { background: #2ea043; }\n" +
                "        .status { margin-top: 5px; font-size: 12px; color: #8b949e; }\n" +
                "        .legend { display: flex; gap: 10px; font-size: 10px; margin-top: 5px; }\n" +
                "        .dot { width: 8px; height: 8px; display: inline-block; border-radius: 50%; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <!-- SIDEBAR GALLERY -->\n" +
                "    <div class=\"sidebar\">\n" +
                "        <div class=\"sidebar-header\">\n" +
                "            <span>📚 Galería</span>\n" +
                "            <button class=\"refresh-btn\" onclick=\"fetchLibrary()\">🔄</button>\n" +
                "        </div>\n" +
                "        <div class=\"video-list\" id=\"libraryList\">\n" +
                "            <div style=\"padding:10px; text-align:center; color:#8b949e\">Cargando...</div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- MAIN WORKBENCH -->\n" +
                "    <div class=\"main-content\">\n" +
                "        <div class=\"container\">\n" +
                "            <h1>🔬 Laboratorio Forense</h1>\n" +
                "            <div class=\"drop-zone\" id=\"dropZone\">📂 ARRASTRA UN VIDEO O SELECCIONA DE LA GALERÍA</div>\n" +
                "            \n" +
                "            <div class=\"lab-bench\" id=\"labBench\">\n" +
                "                <div style=\"text-align:center;\">\n" +
                "                    <div class=\"viewport\">\n" +
                "                        <video id=\"videoSrc\" controls width=\"352\" crossorigin=\"anonymous\"></video>\n" +
                "                        <canvas id=\"motionOverlay\"></canvas>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "\n" +
                "                <div class=\"graph-box\">\n" +
                "                    <svg id=\"graphSvg\" width=\"100%\" height=\"100%\" preserveAspectRatio=\"none\"></svg>\n" +
                "                </div>\n" +
                "                <div class=\"legend\">\n" +
                "                    <span><span class=\"dot\" style=\"background:#238636\"></span> Score</span>\n" +
                "                    <span><span class=\"dot\" style=\"background:#d29922\"></span> Umbral</span>\n" +
                "                    <span><span class=\"dot\" style=\"background:#da3633\"></span> Alerta</span>\n" +
               "<html lang=\"es\">\n" +
               "<head>\n" +
               "            const ctx = canvas.getContext('2d');\n" +
               "            const frames = [];\n" +
               "            let idx = 0;\n" +
               "            let isAnimating = false;\n" +
               "\n" +
               "            fetch(url).then(response => {\n" +
               "                const reader = response.body.getReader();\n" +
               "                let buffer = new Uint8Array(0);\n" +
               "                \n" +
               "                function pump() {\n" +
               "                    reader.read().then(({done, value}) => {\n" +
               "                        if (done) { startAnim(); return; }\n" +
               "                        const newBuffer = new Uint8Array(buffer.length + value.length);\n" +
               "                        newBuffer.set(buffer); newBuffer.set(value, buffer.length);\n" +
               "                        buffer = newBuffer;\n" +
               "                        while(true) {\n" +
               "                            let start = -1; for(let i=0; i<buffer.length-1; i++) { if(buffer[i]===0xFF && buffer[i+1]===0xD8) { start=i; break; } }\n" +
               "                            if(start === -1) break;\n" +
               "                            let end = -1; for(let i=start+2; i<buffer.length-1; i++) { if(buffer[i]===0xFF && buffer[i+1]===0xD9) { end=i+2; break; } }\n" +
               "                            if(end === -1) break;\n" +
               "                            const jpegData = buffer.slice(start, end);\n" +
               "                            const blob = new Blob([jpegData], {type: 'image/jpeg'});\n" +
               "                            const imgUrl = URL.createObjectURL(blob);\n" +
               "                            const img = new Image();\n" +
               "                            img.onload = function() { frames.push(this); URL.revokeObjectURL(imgUrl); if(frames.length === 1) startAnim(); };\n" +
               "                            img.src = imgUrl;\n" +
               "                            buffer = buffer.slice(end);\n" +
               "                        }\n" +
               "                        pump();\n" +
               "                    });\n" +
               "                }\n" +
               "                pump();\n" +
               "            });\n" +
               "\n" +
               "            function startAnim() {\n" +
               "                if(isAnimating) return;\n" +
               "                isAnimating = true;\n" +
               "                function loop() {\n" +
               "                    if(frames.length > 0) {\n" +
               "                        ctx.drawImage(frames[idx], 0, 0, canvas.width, canvas.height);\n" +
               "                        idx = (idx + 1) % frames.length;\n" +
               "                    }\n" +
               "                    setTimeout(() => requestAnimationFrame(loop), 100);\n" +
               "                }\n" +
               "                loop();\n" +
               "            }\n" +
               "        }\n" +
               "\n" +
               "        /* --- WORKBENCH LOGIC --- */\n" +
               "        var video = document.getElementById('videoPlayer');\n" +
               "        var canvas = document.getElementById('analysisCanvas');\n" +
               "        var ctx = canvas.getContext('2d');\n" +
               "        var engine = null;\n" +
               "        var isPlaying = false;\n" +
               "        var animationId;\n" +
               "\n" +
               "        function loadVideo(filename) {\n" +
               "            document.getElementById('current-video-title').textContent = filename;\n" +
               "            video.src = '/video_' + filename;\n" +
               "            video.load();\n" +
               "            log('Loading video: ' + filename);\n" +
               "            \n" +
               "            video.onloadedmetadata = function() {\n" +
               "                canvas.width = video.videoWidth || 640;\n" +
               "                canvas.height = video.videoHeight || 480;\n" +
               "                ctx.drawImage(video, 0, 0, canvas.width, canvas.height);\n" +
               "                engine = new MotionEngine(canvas.width, canvas.height);\n" +
               "                log('Engine initialized (' + canvas.width + 'x' + canvas.height + ')');\n" +
               "            };\n" +
               "        }\n" +
               "\n" +
               "        function togglePlay() {\n" +
               "            if (video.paused) {\n" +
               "                video.play();\n" +
               "                isPlaying = true;\n" +
               "                processLoop();\n" +
               "                document.getElementById('btnPlay').textContent = '⏸ Pause';\n" +
               "            } else {\n" +
               "                video.pause();\n" +
               "                isPlaying = false;\n" +
               "                cancelAnimationFrame(animationId);\n" +
               "                document.getElementById('btnPlay').textContent = '▶ Play';\n" +
               "            }\n" +
               "        }\n" +
               "\n" +
               "        function processLoop() {\n" +
               "            if (!isPlaying || video.paused || video.ended) return;\n" +
               "            // Draw current frame\n" +
               "            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);\n" +
               "            \n" +
               "            // Get Pixels\n" +
               "            var frame = ctx.getImageData(0, 0, canvas.width, canvas.height);\n" +
               "            \n" +
               "            // Run Enzyme\n" +
               "            if (engine) {\n" +
               "                var result = engine.process(frame.data);\n" +
               "                if (result.motion) {\n" +
               "                   document.getElementById('status').textContent = '⚠️ MOVIMIENTO DETECTADO (' + result.pixels + ' px)';\n" +
               "                   drawMotionGrid(result.grid);\n" +
               "                } else {\n" +
               "                   document.getElementById('status').textContent = '...';\n" +
               "                }\n" +
               "            }\n" +
               "            \n" +
               "            animationId = requestAnimationFrame(processLoop);\n" +
               "        }\n" +
               "\n" +
               "        function updateParam(key, val) {\n" +
               "            document.getElementById('val-' + key).textContent = val;\n" +
               "            if (engine) {\n" +
               "                if (key === 'sens') engine.setSensitivity(parseInt(val));\n" +
               "                if (key === 'pix') engine.setMinPixels(parseInt(val));\n" +
               "            }\n" +
               "        }\n" +
               "        \n" +
               "        var debugMode = false;\n" +
               "        function toggleDebug() {\n" +
               "            debugMode = !debugMode;\n" +
               "            document.getElementById('btnDebug').classList.toggle('active');\n" +
               "        }\n" +
               "\n" +
               "        function drawMotionGrid(grid) {\n" +
               "            if(!debugMode) return;\n" +
               "            ctx.fillStyle = 'rgba(255, 0, 0, 0.3)';\n" +
               "            var blockSize = 16;\n" +
               "            // Simple viz (would need real grid data logic here, simplified for now)\n" +
               "            ctx.fillRect(10, 10, 20, 20); // Placeholder\n" +
               "        }\n" +
               "        \n" +
               "        function log(msg) {\n" +
               "            var term = document.getElementById('terminal');\n" +
               "            term.innerHTML += '<div>> ' + msg + '</div>';\n" +
               "            term.scrollTop = term.scrollHeight;\n" +
               "        }\n" +
               "\n" +
               "        // --- JS MOTION ENGINE (Ported from Java) ---\n" +
               "        class MotionEngine {\n" +
               "            constructor(w, h) {\n" +
               "                this.width = w;\n" +
               "                this.height = h;\n" +
               "                this.prevFrame = null;\n" +
               "                this.threshold = 15;\n" +
               "                this.minPixels = 20;\n" +
               "            }\n" +
               "            setSensitivity(val) { this.threshold = val; log('Sensitivity set to ' + val); }\n" +
               "            setMinPixels(val) { this.minPixels = val; log('Min Pixels set to ' + val); }\n" +
               "\n" +
               "            process(currentData) {\n" +
               "                if (!this.prevFrame) {\n" +
               "                    this.prevFrame = new Uint8ClampedArray(currentData);\n" +
               "                    return { motion: false };\n" +
               "                }\n" +
               "                \n" +
               "                let motionPixels = 0;\n" +
               "                // Simple Grayscale Diff (Step 4)\n" +
               "                for (let i = 0; i < currentData.length; i += 4) {\n" +
               "                    let r1 = currentData[i], g1 = currentData[i+1], b1 = currentData[i+2];\n" +
               "                    let r2 = this.prevFrame[i], g2 = this.prevFrame[i+1], b2 = this.prevFrame[i+2];\n" +
               "                    \n" +
               "                    let lum1 = 0.299*r1 + 0.587*g1 + 0.114*b1;\n" +
               "                    let lum2 = 0.299*r2 + 0.587*g2 + 0.114*b2;\n" +
               "                    \n" +
               "                    if (Math.abs(lum1 - lum2) > this.threshold) {\n" +
               "                        motionPixels++;\n" +
               "                        // Highlight (Debug)\n" +
               "                        if(debugMode) { currentData[i] = 255; currentData[i+1]=0; currentData[i+2]=0; }\n" +
               "                    }\n" +
               "                }\n" +
               "                \n" +
               "                // Update prev\n" +
               "                this.prevFrame.set(currentData);\n" +
               "                \n" +
               "                return { \n" +
               "                    motion: motionPixels > this.minPixels,\n" +
               "                    pixels: motionPixels\n" +
               "                };\n" +
               "            }\n" +
               "        }\n" +
               "    </script>\n" +
               "</body>\n" +
               "</html>";
    }
}
