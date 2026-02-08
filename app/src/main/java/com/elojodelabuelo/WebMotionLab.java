package com.elojodelabuelo;

import java.util.Locale;

public class WebMotionLab {

    public static String getHtml() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"es\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <title>Motion Lab 🧪</title>\n" +
               "    <style>\n" +
               "        body { margin:0; font-family: sans-serif; background: #121212; color: #e0e0e0; display:flex; flex-direction:column; height:100vh; overflow:hidden; }\n" +
               "        \n" +
               "        /* --- PAGE 1: GALLERY --- */\n" +
               "        #gallery-page { display:none; flex-direction:column; height:100%; padding:20px; overflow-y:auto; box-sizing:border-box; }\n" +
               "        .gallery-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }\n" +
               "        .gallery-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px; }\n" +
               "        \n" +
               "        .video-card { background: #1e1e1e; border-radius: 8px; overflow: hidden; cursor: pointer; transition: transform 0.2s; border:1px solid #333; position:relative; }\n" +
               "        .video-card:hover { transform: scale(1.02); border-color:#0288d1; }\n" +
               "        \n" +
               "        /* FIX: Android 4.4 doesn't support aspect-ratio. Use padding hack. */\n" +
               "        .media-container { position: relative; width: 100%; padding-bottom: 56.25%; /* 16:9 */ background: #000; overflow:hidden; }\n" +
               "        .v-thumb, .v-canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; display:block; }\n" +
               "        \n" +
               "        .v-info { padding: 8px; font-size: 12px; color: #b0b0b0; }\n" +
               "        .v-name { font-weight: bold; color: #fff; display:block; margin-bottom:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }\n" +
               "\n" +
               "        /* --- PAGE 2: WORKBENCH --- */\n" +
               "        #workbench-page { display:none; flex-direction:row; height:100%; }\n" +
               "        .main-content { flex: 1; display: flex; flex-direction: column; padding: 10px; box-sizing: border-box; background:#000; }\n" +
               "        .top-bar { display:flex; align-items:center; padding: 5px 10px; background:#1e1e1e; border-bottom:1px solid #333; height:40px; }\n" +
               "        .back-btn { background:none; border:none; color:#bbb; font-size:18px; cursor:pointer; margin-right:15px; padding:5px; }\n" +
               "        .back-btn:hover { color:#fff; }\n" +
               "\n" +
               "        canvas { background: #000; border: 1px solid #333; max-width: 100%; max-height: calc(100vh - 250px); }\n" +
               "        .controls { display: flex; gap: 10px; padding: 10px; background: #1e1e1e; align-items: center; border-top:1px solid #333; }\n" +
               "        button { background: #333; color: white; border: none; padding: 8px 12px; cursor: pointer; border-radius: 4px; }\n" +
               "        button:hover { background: #444; }\n" +
               "        button.active { background: #0288d1; }\n" +
               "        .param-group { display: flex; flex-direction: column; gap: 5px; margin-left:15px; border-left:1px solid #444; padding-left:15px; }\n" +
               "        label { font-size: 11px; color: #888; }\n" +
               "        input[type=range] { width: 100px; }\n" +
               "        #status { margin-left: auto; font-family: monospace; color: #0f0; font-size:12px; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <!-- PAGE 1: GALLERY -->\n" +
               "    <div id=\"gallery-page\">\n" +
               "        <div class=\"gallery-header\">\n" +
               "            <h2 style=\"margin:0\">📚 Galería de Grabaciones</h2>\n" +
               "            <button onclick=\"fetchLibrary()\" style=\"background:#0288d1\">🔄 Actualizar</button>\n" +
               "        </div>\n" +
               "        <div class=\"gallery-grid\" id=\"libraryList\">\n" +
               "            <div style=\"padding:20px; text-align:center; color:#666; grid-column:1/-1\">Cargando videos...</div>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "\n" +
               "    <!-- PAGE 2: WORKBENCH -->\n" +
               "    <div id=\"workbench-page\">\n" +
               "        <div class=\"main-content\">\n" +
               "            <div class=\"top-bar\">\n" +
               "                <button class=\"back-btn\" onclick=\"goBack()\">⬅ Volver</button>\n" +
               "                <span id=\"current-video-title\" style=\"font-weight:bold; color:#fff\">Sin Video</span>\n" +
               "                <span id=\"status\">Simulación Inactiva</span>\n" +
               "            </div>\n" +
               "            \n" +
               "            <video id=\"videoPlayer\" controls style=\"display:none\"></video>\n" +
               "            <!-- Canvas de Análisis -->\n" +
               "            <canvas id=\"analysisCanvas\" width=\"640\" height=\"480\"></canvas>\n" +
               "\n" +
               "            <div class=\"controls\">\n" +
               "                <button onclick=\"togglePlay()\" id=\"btnPlay\">⏯ Play/Pause</button>\n" +
               "                \n" +
               "                <div class=\"param-group\">\n" +
               "                    <label>🎨 Sensibilidad al Contraste: <span id=\"val-contrast\">50</span></label>\n" +
               "                    <input type=\"range\" min=\"0\" max=\"100\" value=\"50\" oninput=\"updateParam('contrast', this.value)\">\n" +
               "                </div>\n" +
               "                \n" +
               "                <div class=\"param-group\">\n" +
               "                    <label>📏 Sensibilidad al Tamaño: <span id=\"val-pix\">50</span></label>\n" +
               "                    <input type=\"range\" min=\"5\" max=\"200\" value=\"50\" oninput=\"updateParam('pix', this.value)\">\n" +
               "                </div>\n" +
               "                \n" +
               "                <div class=\"param-group\">\n" +
               "                    <label>Debug Mode</label>\n" +
               "                    <button onclick=\"toggleDebug()\" id=\"btnDebug\">👁️ Ver Malla</button>\n" +
               "                </div>\n" +
               "            </div>\n" +
               "            \n" +
               "            <!-- Terminal Output -->\n" +
               "            <div id=\"terminal\" style=\"background:#000; color:#0f0; font-family:monospace; padding:10px; font-size:11px; height:100px; overflow-y:auto; border-top:1px solid #333; margin-top:5px;\">\n" +
               "                > Web Motion Lab v3.9.10 ready.\n" +
               "            </div>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "\n" +
               "    <script>\n" +
               "        /* --- GLOBALS --- */\n" +
               "        var video = document.getElementById('videoPlayer');\n" +
               "        var canvas = document.getElementById('analysisCanvas');\n" +
               "        var ctx = canvas.getContext('2d');\n" +
               "        var engine = null;\n" +
               "        var isPlaying = false;\n" +
               "        var animationId;\n" +
               "        var debugMode = false;\n" +
               "\n" +
               "        /* --- ROUTING & INIT --- */\n        // Moved to bottom to fix Hoisting\n" +
               "\n" +
               "        function showGallery() {\n" +
               "            document.getElementById('gallery-page').style.display = 'flex';\n" +
               "            document.getElementById('workbench-page').style.display = 'none';\n" +
               "            fetchLibrary();\n" +
               "        }\n" +
               "\n" +
               "        function showWorkbench(filename) {\n" +
               "            document.getElementById('gallery-page').style.display = 'none';\n" +
               "            document.getElementById('workbench-page').style.display = 'flex';\n" +
               "            // Update URL without reload if not there\n" +
               "            if(!videoParam) {\n" +
               "                const newUrl = window.location.protocol + '//' + window.location.host + window.location.pathname + '?video=' + filename;\n" +
               "                window.history.pushState({path:newUrl},'',newUrl);\n" +
               "            }\n" +
               "            loadVideo(filename);\n" +
               "        }\n" +
               "\n" +
               "        function goBack() {\n" +
               "            // Go to root\n" +
               "            window.location.href = window.location.pathname;\n" +
               "        }\n" +
               "\n" +
               "        /* --- GALLERY LOGIC --- */\n" +
               "        async function fetchLibrary() {\n" +
               "            const list = document.getElementById('libraryList');\n" +
               "            try {\n" +
               "                const resp = await fetch('/api/list_videos?limit=50');\n" +
               "                const videos = await resp.json();\n" +
               "                \n" +
               "                list.innerHTML = '';\n" +
               "                if (videos.length === 0) list.innerHTML = '<div style=\"padding:20px;grid-column:1/-1;text-align:center\">Sin videos</div>';\n" +
               "                \n" +
               "                videos.forEach(v => {\n" +
               "                    const div = document.createElement('div');\n" +
               "                    div.className = 'video-card';\n" +
               "                    \n" +
               "                    let mediaHtml = '';\n" +
               "                    if (v.preview) {\n" +
               "                         mediaHtml = `<canvas class=\"v-canvas\" width=\"320\" height=\"240\" data-src=\"/${v.preview}\"></canvas>`;\n" +
               "                    } else if (v.thumb) {\n" +
               "                         mediaHtml = `<img src=\"/thumbnails/${v.thumb}\" class=\"v-thumb\">`;\n" +
               "                    } else {\n" +
               "                         mediaHtml = `<div class=\"v-thumb\" style=\"background:#333\"></div>`;\n" +
               "                    }\n" +
               "\n" +
               "                    div.innerHTML = `\n" +
               "                        <div class=\"media-container\">\n" +
               "                            ${mediaHtml}\n" +
               "                        </div>\n" +
               "                        <div class=\"v-info\">\n" +
               "                            <span class=\"v-name\">${v.name}</span>\n" +
               "                            <span class=\"v-meta\">${v.date} | ${v.size}</span>\n" +
               "                        </div>\n" +
               "                    `;\n" +
               "                    // Navigate to Workbench\n" +
               "                    div.onclick = () => window.location.href = '?video=' + v.name;\n" +
               "                    list.appendChild(div);\n" +
               "\n" +
               "                    if(v.preview) {\n" +
               "                        const cnv = div.querySelector('canvas');\n" +
               "                        loadMiniPreview(`/${v.preview}`, cnv);\n" +
               "                    }\n" +
               "                });\n" +
               "            } catch (e) {\n" +
               "                list.innerHTML = '<div style=\"color:red;padding:10px\">Error API</div>';\n" +
               "            }\n" +
               "        }\n" +
               "\n" +
               "        // --- MJPEG PLAYER (Dashboard Port) ---\n" +
               "        class MJPEGPlayer {\n" +
               "            constructor(url, canvas) {\n" +
               "                this.url = url;\n" +
               "                this.canvas = canvas;\n" +
               "                this.ctx = canvas.getContext('2d');\n" +
               "                this.frames = [];\n" +
               "                this.frameIdx = 0;\n" +
               "                this.isPlaying = false;\n" +
               "                this.abortController = null;\n" +
               "                this.loopId = null;\n" +
               "                this.onFrame = null; // Callback for MotionEngine\n" +
               "            }\n" +
               "\n" +
               "            async load() {\n" +
               "                this.stop();\n" +
               "                this.frames = [];\n" +
               "                this.frameIdx = 0;\n" +
               "                this.abortController = new AbortController();\n" +
               "                log('Fetching MJPEG: ' + this.url);\n" +
               "\n" +
               "                try {\n" +
               "                    const response = await fetch(this.url, { signal: this.abortController.signal });\n" +
               "                    if (!response.ok) throw new Error('Network response was not ok');\n" +
               "                    \n" +
               "                    const reader = response.body.getReader();\n" +
               "                    let buffer = new Uint8Array(0);\n" +
               "                    let validFrames = 0;\n" +
               "\n" +
               "                    const processBuffer = async () => {\n" +
               "                        while (true) {\n" +
               "                            // Find SOI FFD8\n" +
               "                            let start = -1;\n" +
               "                            for (let i = 0; i < buffer.length - 1; i++) {\n" +
               "                                if (buffer[i] === 0xFF && buffer[i + 1] === 0xD8) { start = i; break; }\n" +
               "                            }\n" +
               "                            if (start === -1) break; // Need more data\n" +
               "\n" +
               "                            // Find EOI FFD9\n" +
               "                            let end = -1;\n" +
               "                            for (let i = start + 2; i < buffer.length - 1; i++) {\n" +
               "                                if (buffer[i] === 0xFF && buffer[i + 1] === 0xD9) { end = i + 2; break; }\n" +
               "                            }\n" +
               "                            if (end === -1) break; // Need more data\n" +
               "\n" +
               "                            // Extract Frame\n" +
               "                            const jpegData = buffer.slice(start, end);\n" +
               "                            buffer = buffer.slice(end); // Advance buffer\n" +
               "\n" +
               "                            // Create Image Bitmap (Async)\n" +
               "                            const blob = new Blob([jpegData], { type: 'image/jpeg' });\n" +
               "                            const bmp = await createImageBitmap(blob);\n" +
               "                            this.frames.push(bmp);\n" +
               "                            validFrames++;\n" +
               "                            \n" +
               "                            // Auto-Start on first frame? No, wait for user or buffer some\n" +
               "                            if (validFrames === 1) {\n" +
               "                                this.drawFrame(0);\n" +
               "                                if (engine) {\n" +
               "                                    engine.width = bmp.width;\n" +
               "                                    engine.height = bmp.height;\n" +
               "                                    log('Engine initialized (' + bmp.width + 'x' + bmp.height + ')');\n" +
               "                                }\n" +
               "                            }\n" +
               "                        }\n" +
               "                    };\n" +
               "\n" +
               "                    const pump = async () => {\n" +
               "                        const { done, value } = await reader.read();\n" +
               "                        if (done) {\n" +
               "                            log('Download complete. Total Frames: ' + this.frames.length);\n" +
               "                            return;\n" +
               "                        }\n" +
               "                        // Append new data\n" +
               "                        const newBuffer = new Uint8Array(buffer.length + value.length);\n" +
               "                        newBuffer.set(buffer);\n" +
               "                        newBuffer.set(value, buffer.length);\n" +
               "                        buffer = newBuffer;\n" +
               "                        \n" +
               "                        await processBuffer();\n" +
               "                        pump();\n" +
               "                    };\n" +
               "                    pump();\n" +
               "\n" +
               "                } catch (err) {\n" +
               "                    if (err.name === 'AbortError') log('Download aborted');\n" +
               "                    else log('Error loading video: ' + err.message);\n" +
               "                }\n" +
               "            }\n" +
               "\n" +
               "            play() {\n" +
               "                if (this.isPlaying) return;\n" +
               "                this.isPlaying = true;\n" +
               "                this.loop();\n" +
               "                document.getElementById('btnPlay').textContent = '⏸ Pause';\n" +
               "            }\n" +
               "\n" +
               "            pause() {\n" +
               "                this.isPlaying = false;\n" +
               "                if (this.loopId) cancelAnimationFrame(this.loopId);\n" +
               "                document.getElementById('btnPlay').textContent = '▶ Play';\n" +
               "            }\n" +
               "\n" +
               "            stop() {\n" +
               "                this.pause();\n" +
               "                if (this.abortController) this.abortController.abort();\n" +
               "                this.frames = [];\n" +
               "                this.frameIdx = 0;\n" +
               "            }\n" +
               "\n" +
               "            loop() {\n" +
               "                if (!this.isPlaying) return;\n" +
               "                \n" +
               "                // Draw current frame\n" +
               "                if (this.frames.length > 0) {\n" +
               "                    this.drawFrame(this.frameIdx);\n" +
               "                    \n" +
               "                    // Callback for Engine\n" +
               "                    if (this.onFrame) {\n" +
               "                        try {\n" +
               "                            const frameData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);\n" +
               "                            this.onFrame(frameData.data);\n" +
               "                            // Visual Debug: Repaint modified pixels if debug is on\n" +
               "                            if (window.debugMode) {\n" +
               "                                this.ctx.putImageData(frameData, 0, 0);\n" +
               "                            }\n" +
               "                        } catch(e) { console.error(e); }\n" +
               "                    }\n" +
               "\n" +
               "                    // Next frame\n" +
               "                    this.frameIdx = (this.frameIdx + 1) % this.frames.length;\n" +
               "                }\n" +
               "\n" +
               "                // Throttle? MJPEG usually 5-15 FPS. \n" +
               "                // Let's use setTimeout to simulate FPS, or just queryAnimationFrame at max speed?\n" +
               "                // Max speed might be too fast. Let's aim for ~15 FPS (66ms)\n" +
               "                setTimeout(() => {\n" +
               "                    this.loopId = requestAnimationFrame(() => this.loop());\n" +
               "                }, 66);\n" +
               "            }\n" +
               "\n" +
               "            drawFrame(idx) {\n" +
               "                if (this.frames[idx]) {\n" +
               "                    // Update canvas size if needed? No, user set fixed size?\n" +
               "                    // Or follow video? user said \"640x480\"\n" +
               "                    this.ctx.drawImage(this.frames[idx], 0, 0, this.canvas.width, this.canvas.height);\n" +
               "                }\n" +
               "            }\n" +
               "        }\n" +
               "\n" +
               "        // === MINI MJPEG PLAYER (Dashboard Port) ===\n" +
               "        function loadMiniPreview(url, canvas) {\n" +
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
               "        // Vars moved to top\n" +
               "        \n" +
               "        function loadVideo(filename) {\n" +
               "            document.getElementById('current-video-title').textContent = filename;\n" +
               "            \n" +
               "            // Clean up old player if exists? Global var video is element, not class instance.\n" +
               "            // We need a global instance of MJPEGPlayer\n" +
               "            if (window.mjpegPlayer) window.mjpegPlayer.stop();\n" +
               "            \n" +
               "            window.mjpegPlayer = new MJPEGPlayer('/' + filename, canvas);\n" +
               "            window.mjpegPlayer.onFrame = (data) => {\n" +
               "                if (engine) {\n" +
               "                    var result = engine.process(data);\n" +
               "                    const statusEl = document.getElementById('status');\n" +
               "                    \n" +
               "                    if (result.filtered) {\n" +
               "                       statusEl.textContent = '💡 CAMBIO DE LUZ (Ignorado)';\n" +
               "                       statusEl.style.color = '#FFA500'; // Orange\n" +
               "                    } else if (result.motion) {\n" +
               "                       statusEl.textContent = '⚠️ MOVIMIENTO DETECTADO (' + result.pixels + ' px)';\n" +
               "                       statusEl.style.color = '#FF0000'; // Red\n" +
               "                    } else {\n" +
               "                       statusEl.textContent = '...';\n" +
               "                       statusEl.style.color = '#888';\n" +
               "                    }\n" +
               "                }\n" +
               "            };\n" +
               "            window.mjpegPlayer.load();\n" +
               "            \n" +
               "            // Hide native video element, we use Canvas only\n" +
               "            video.style.display = 'none';\n" +
               "        }\n" +
               "\n" +
               "        function togglePlay() {\n" +
               "            if (window.mjpegPlayer) {\n" +
               "                if (window.mjpegPlayer.isPlaying) window.mjpegPlayer.pause();\n" +
               "                else window.mjpegPlayer.play();\n" +
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
               "            window.mjpegPlayer.onFrame = (data) => {\n" +
               "                if (engine) {\n" +
               "                    var result = engine.process(data);\n" +
               "                    const statusEl = document.getElementById('status');\n" +
               "                    \n" +
               "                    if (result.filtered) {\n" +
               "                       statusEl.textContent = '💡 CAMBIO DE LUZ (Ignorado)';\n" +
               "                       statusEl.style.color = '#FFA500'; // Orange\n" +
               "                    } else if (result.motion) {\n" +
               "                       statusEl.textContent = '⚠️ MOVIMIENTO DETECTADO (' + result.pixels + ' px)';\n" +
               "                       statusEl.style.color = '#FF0000'; // Red\n" +
               "                    } else {\n" +
               "                       statusEl.textContent = '...';\n" +
               "                       statusEl.style.color = '#888';\n" +
               "                    }\n" +
               "                }\n" +
               "            };\n" +
               "            \n" +
               "            animationId = requestAnimationFrame(processLoop);\n" +
               "        }\n" +
               "\n" +
               "        function updateParam(key, val) {\n" +
               "            document.getElementById('val-' + key).textContent = val;\n" +
               "            if (engine) {\n" +
               "                if (key === 'contrast') { var t = 105 - parseInt(val); if(t<5)t=5; if(t>100)t=100; engine.setSensitivity(t); }\n" +
               "                if (key === 'pix') engine.setMinPixels(parseInt(val));\n" +
               "            }\n" +
               "        }\n" +
               "        \n" +
               "        function toggleDebug() {\n" +
               "            debugMode = !debugMode;\n" +
               "            document.getElementById('btnDebug').classList.toggle('active');\n" +
               "        }\n" +
               "\n" +
               "        // Deprecated: Viz is now pixel-level in MotionEngine\n" +
               "        function drawMotionGrid(grid) {}\n" +
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
               "                \n" +
               "                // Correspondencia exacta con MotionDetector.java\n" +
               "                this.STRIDE = 10;\n" +
               "                this.THRESHOLD = 50; \n" +
               "                this.MOTION_PIXEL_COUNT = 50;\n" +
               "                this.LIGHT_CHANGE_RATIO = 0.60;\n" +
               "                this.smartFilterEnabled = true;\n" +
               "                \n" +
               "                log('MotionEngine initialized (Stride: '+this.STRIDE+', Threshold: '+this.THRESHOLD+')');\n" +
               "            }\n" +
               "            \n" +
               "            setSensitivity(val) { this.THRESHOLD = val; log('Sensitivity (Thresh) set to ' + val); }\n" +
               "            setMinPixels(val) { this.MOTION_PIXEL_COUNT = val; log('Min Pixels set to ' + val); }\n" +
               "            setSmartFilter(enabled) { this.smartFilterEnabled = enabled; log('Smart Filter: ' + enabled); }\n" +
               "\n" +
               "            process(currentData) {\n" +
               "                // currentData is Uint8ClampedArray (RGBA)\n" +
               "                // We need to simulate YUV luminance check\n" +
               "                \n" +
               "                if (!this.prevFrame) {\n" +
               "                    this.prevFrame = new Uint8ClampedArray(currentData);\n" +
               "                    return { motion: false, pixels: 0, debug: 'Init' };\n" +
               "                }\n" +
               "                \n" +
               "                let diffCount = 0;\n" +
               "                let totalPixelsChecked = 0;\n" +
               "                \n" +
               "                // Stride Loop (matches Java loop logic)\n" +
               "                // Java: for (int i = 0; i < limit; i += STRIDE)\n" +
               "                // JS Data is RGBA (4 bytes per pixel). \n" +
               "                // Stride 10 means skip 10 pixels -> skip 40 bytes\n" +
               "                const byteStride = this.STRIDE * 4;\n" +
               "                \n" +
               "                for (let i = 0; i < currentData.length; i += byteStride) {\n" +
               "                    totalPixelsChecked++;\n" +
               "                    \n" +
               "                    // Extract Luminance (Y)\n" +
               "                    // Y = 0.299*R + 0.587*G + 0.114*B\n" +
               "                    let r1 = currentData[i];\n" +
               "                    let g1 = currentData[i+1];\n" +
               "                    let b1 = currentData[i+2];\n" +
               "                    let val1 = 0.299*r1 + 0.587*g1 + 0.114*b1;\n" +
               "\n" +
               "                    let r2 = this.prevFrame[i];\n" +
               "                    let g2 = this.prevFrame[i+1];\n" +
               "                    let b2 = this.prevFrame[i+2];\n" +
               "                    let val2 = 0.299*r2 + 0.587*g2 + 0.114*b2;\n" +
               "                    \n" +
               "                    if (Math.abs(val1 - val2) > this.THRESHOLD) {\n" +
               "                        diffCount++;\n" +
               "                        // Debug Highlight (Red)\n" +
               "                        if (debugMode) {\n" +
               "                            // Mark the pixel red in currentData for visualization\n" +
               "                            // Note: This modifies the frame being displayed! \n" +
               "                            currentData[i] = 255;   // R\n" +
               "                            currentData[i+1] = 0;   // G\n" +
               "                            currentData[i+2] = 0;   // B\n" +
               "                        }\n" +
               "                    }\n" +
               "                }\n" +
               "                \n" +
               "                // --- SMART FILTER LOGIC ---\n" +
               "                let filtered = false;\n" +
               "                if (this.smartFilterEnabled && totalPixelsChecked > 0) {\n" +
               "                    const changeRatio = diffCount / totalPixelsChecked;\n" +
               "                    if (changeRatio > this.LIGHT_CHANGE_RATIO) {\n" +
               "                        // Light Change Detected (>60%)\n" +
               "                        // Ignore motion, update reference\n" +
               "                        diffCount = 0;\n" +
               "                        filtered = true;\n" +
               "                        // log('💡 Smart Filter Triggered! (Global Change)');\n" +
               "                    }\n" +
               "                }\n" +
               "                \n" +
               "                // Update Reference Frame\n" +
               "                // In Java: System.arraycopy(currentFrame, 0, previousFrame, 0, currentFrame.length);\n" +
               "                if (!filtered) {\n" +
               "                    this.prevFrame.set(currentData);\n" +
               "                }\n" +
               "                \n" +
               "                return { \n" +
               "                    motion: diffCount > this.MOTION_PIXEL_COUNT,\n" +
               "                    pixels: diffCount,\n" +
               "                    filtered: filtered\n" +
               "                };\n" +
               "            }\n" +
               "        }\n" +
               "\n" +
               "        /* --- ROUTING & INIT (Moved here) --- */\n" +
               "        const urlParams = new URLSearchParams(window.location.search);\n" +
               "        const videoParam = urlParams.get('video');\n" +
               "\n" +
               "        if (videoParam) {\n" +
               "            showWorkbench(videoParam);\n" +
               "        } else {\n" +
               "            showGallery();\n" +
               "        }\n" +
               "    </script>\n" +
               "</body>\n" +
               "</html>";
    }
}
