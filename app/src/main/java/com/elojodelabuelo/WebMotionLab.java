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
                "                </div>\n" +
                "\n" +
                "                <div class=\"controls\">\n" +
                "                    <div class=\"control-row\">\n" +
                "                        <label>⚡ Sensibilidad (0-100):</label>\n" +
                "                        <input type=\"range\" id=\"sensRange\" min=\"0\" max=\"100\" value=\"80\">\n" +
                "                        <span class=\"val-disp\" id=\"sensVal\">80</span>\n" +
                "                    </div>\n" +
                "                    <div class=\"control-row\">\n" +
                "                        <label>💡 Filtro Anti-Luz (60%):</label>\n" +
                "                        <label><input type=\"checkbox\" id=\"slFilter\" checked> Activar</label>\n" +
                "                    </div>\n" +
                "                    <button class=\"btn\" id=\"btnAnalyze\">🔄 RE-ANALIZAR AHORA</button>\n" +
                "                    <div class=\"status\" id=\"statusMsg\">Listo para simular.</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        // === MOTOR DE DETECCIÓN JS (PORT DEL JAVA) ===\n" +
                "        class MotionEngine {\n" +
                "            constructor() {\n" +
                "                this.width = 0;\n" +
                "                this.height = 0;\n" +
                "                this.prevFrame = null;\n" +
                "                this.stride = 2; // Pixel skipping optimization\n" +
                "            }\n" +
                "\n" +
                "            init(w, h) {\n" +
                "                this.width = w;\n" +
                "                this.height = h;\n" +
                "                this.prevFrame = new Uint8Array(w * h);\n" +
                "            }\n" +
                "\n" +
                "            processFrame(imgData, sens, smartFilter) {\n" +
                "                let w = this.width;\n" +
                "                let h = this.height;\n" +
                "                let data = imgData.data; // RGBA\n" +
                "                let diffCount = 0;\n" +
                "                let totalChecked = 0;\n" +
                "                let diffMap = []; \n" +
                "\n" +
                "                // Java: int threshold = (100 - sensitivity) * 255 / 100;\n" +
                "                // Ajuste web: \n" +
                "                let threshold = Math.floor((100 - sens) * 2.55);\n" +
                "\n" +
                "                for (let i = 0; i < (w * h); i += this.stride) {\n" +
                "                    let r = data[i * 4];\n" +
                "                    let g = data[i * 4 + 1];\n" +
                "                    let b = data[i * 4 + 2];\n" +
                "                    \n" +
                "                    let luma = Math.floor(0.299 * r + 0.587 * g + 0.114 * b);\n" +
                "                    let prev = this.prevFrame[i];\n" +
                "                    \n" +
                "                    if (Math.abs(luma - prev) > threshold) {\n" +
                "                        diffCount++;\n" +
                "                        diffMap.push(i);\n" +
                "                    }\n" +
                "                    \n" +
                "                    this.prevFrame[i] = luma;\n" +
                "                    totalChecked++;\n" +
                "                }\n" +
                "\n" +
                "                let ratio = diffCount / totalChecked;\n" +
                "                let isLight = false;\n" +
                "                if (smartFilter && ratio > 0.60) {\n" +
                "                    isLight = true;\n" +
                "                }\n" +
                "\n" +
                "                return {\n" +
                "                    score: isLight ? 0 : diffCount,\n" +
                "                    rawDiff: diffCount,\n" +
                "                    ratio: ratio,\n" +
                "                    isLight: isLight,\n" +
                "                    diffMap: diffMap\n" +
                "                };\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        // === UI LOGIC ===\n" +
                "        const video = document.getElementById('videoSrc');\n" +
                "        const canvas = document.getElementById('motionOverlay');\n" +
                "        const ctx = canvas.getContext('2d');\n" +
                "        const engine = new MotionEngine();\n" +
                "        \n" +
                "        // Eventos\n" +
                "        document.getElementById('dropZone').addEventListener('dragover', (e) => { e.preventDefault(); });\n" +
                "        document.getElementById('dropZone').addEventListener('drop', handleDrop);\n" +
                "        document.getElementById('btnAnalyze').addEventListener('click', runSimulation);\n" +
                "        document.getElementById('sensRange').addEventListener('input', (e) => {\n" +
                "            document.getElementById('sensVal').innerText = e.target.value;\n" +
                "        });\n" +
                "\n" +
                "        // --- INIT ---\n" +
                "        fetchLibrary();\n" +
                "\n" +
                "        // --- LIBRARY LOGIC ---\n" +
                "        async function fetchLibrary() {\n" +
                "            const list = document.getElementById('libraryList');\n" +
                "            list.innerHTML = '<div style=\"padding:10px;text-align:center\">Cargando...</div>';\n" +
                "            \n" +
                "            try {\n" +
                "                const resp = await fetch('/api/list_videos?limit=50');\n" +
                "                const videos = await resp.json();\n" +
                "                \n" +
                "                list.innerHTML = '';\n" +
                "                if (videos.length === 0) list.innerHTML = '<div style=\"padding:10px\">Sin videos</div>';\n" +
                "                \n" +
                "                videos.forEach(v => {\n" +
                "                    const div = document.createElement('div');\n" +
                "                    div.className = 'video-card';\n" +
                "                    \n" +
                "                    // Show Preview (Autoplay) if available, else Thumbnail\n" +
                "                    let mediaHtml = '';\n" +
                "                    if (v.preview) {\n" +
                "                         // Autoplay video preview (muted, loop)\n" +
                "                         mediaHtml = `<img src=\"/${v.preview}\" class=\"v-thumb\" style=\"object-fit:cover\">`;\n" +
                "                    } else if (v.thumb) {\n" +
                "                         mediaHtml = `<img src=\"/thumbnails/${v.thumb}\" class=\"v-thumb\">`;\n" +
                "                    } else {\n" +
                "                         mediaHtml = `<div class=\"v-thumb\" style=\"background:#333\"></div>`;\n" +
                "                    }\n" +
                "\n" +
                "                    div.innerHTML = `\n" +
                "                        ${mediaHtml}\n" +
                "                        <div class=\"v-info\">\n" +
                "                            <span class=\"v-name\">${v.name}</span>\n" +
                "                            <span class=\"v-meta\">${v.date} | ${v.size}</span>\n" +
                "                        </div>\n" +
                "                    `;\n" +
                "                    div.onclick = () => loadVideoFromLib(v.name, div);\n" +
                "                    list.appendChild(div);\n" +
                "                });\n" +
                "            } catch (e) {\n" +
                "                list.innerHTML = '<div style=\"color:red;padding:10px\">Error API</div>';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function loadVideoFromLib(filename, cardElem) {\n" +
                "            // Highlight Active\n" +
                "            document.querySelectorAll('.video-card').forEach(c => c.classList.remove('active'));\n" +
                "            cardElem.classList.add('active');\n" +
                "            \n" +
                "            // Load Video\n" +
                "            const url = '/video_' + filename;\n" +
                "            video.src = url;\n" +
                "            prepareWorkbench(filename);\n" +
                "        }\n" +
                "\n" +
                "        function handleDrop(e) {\n" +
                "            e.preventDefault();\n" +
                "            const file = e.dataTransfer.files[0];\n" +
                "            if (file) {\n" +
                "                const url = URL.createObjectURL(file);\n" +
                "                video.src = url;\n" +
                "                prepareWorkbench(file.name);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function prepareWorkbench(name) {\n" +
                "            document.getElementById('labBench').style.display = 'block';\n" +
                "            document.getElementById('dropZone').style.display = 'none';\n" +
                "            \n" +
                "            // Limpiar gráfica anterior\n" +
                "            document.getElementById('graphSvg').innerHTML = '';\n" +
                "            document.getElementById('statusMsg').innerText = `Video cargado: ${name}. Pulsa ANALIZAR.`;\n" +
                "            \n" +
                "            video.onloadedmetadata = () => {\n" +
                "                canvas.width = video.videoWidth;\n" +
                "                canvas.height = video.videoHeight;\n" +
                "                engine.init(video.videoWidth, video.videoHeight);\n" +
                "            };\n" +
                "        }\n" +
                "\n" +
                "        async function runSimulation() {\n" +
                "            if (!video.duration && !video.readyState) return;\n" +
                "            \n" +
                "            const status = document.getElementById('statusMsg');\n" +
                "            status.innerText = '⏳ Analizando frame a frame (Turbo)...';\n" +
                "            \n" +
                "            const sens = parseInt(document.getElementById('sensRange').value);\n" +
                "            const smart = document.getElementById('slFilter').checked;\n" +
                "            \n" +
                "            // limit = 10000 * (1 - sens/100)^2\n" +
                "            const motionThreshold = 10000 * Math.pow((1 - sens/100.0), 2);\n" +
                "\n" +
                "            const history = [];\n" +
                "            \n" +
                "            // Hack: Reproducción acelerada oculta\n" +
                "            const offVideo = document.createElement('video');\n" +
                "            offVideo.crossOrigin = 'anonymous'; // Vital para videos del servidor\n" +
                "            offVideo.src = video.src;\n" +
                "            offVideo.muted = true;\n" +
                "            offVideo.currentTime = 0;\n" +
                "            \n" +
                "            const tmpCvn = document.createElement('canvas');\n" +
                "            tmpCvn.width = canvas.width;\n" +
                "            tmpCvn.height = canvas.height;\n" +
                "            const tmpCtx = tmpCvn.getContext('2d', { willReadFrequently: true });\n" +
                "            \n" +
                "            try {\n" +
                "                await offVideo.play();\n" +
                "                offVideo.pause();\n" +
                "\n" +
                "                const duration = offVideo.duration || 10;\n" +
                "                const fps = 10; // Bajamos FPS de análisis para más velocidad\n" +
                "                const step = 1/fps;\n" +
                "                \n" +
                "                for (let t = 0; t < duration; t += step) {\n" +
                "                    offVideo.currentTime = t;\n" +
                "                    await new Promise(r => offVideo.onseeked = r);\n" +
                "                    \n" +
                "                    tmpCtx.drawImage(offVideo, 0, 0, canvas.width, canvas.height);\n" +
                "                    const frame = tmpCtx.getImageData(0, 0, canvas.width, canvas.height);\n" +
                "                    \n" +
                "                    const res = engine.processFrame(frame, sens, smart);\n" +
                "                    history.push({ t: t, s: res.score, l: res.isLight });\n" +
                "\n" +
                "                    if (t % 1 < step) status.innerText = `Analizando: ${Math.round(t)}s / ${Math.round(duration)}s`;\n" +
                "                }\n" +
                "\n" +
                "                drawGraph(history, motionThreshold);\n" +
                "                status.innerText = '✅ Análisis Completo.';\n" +
                "            } catch (e) {\n" +
                "                console.error(e);\n" +
                "                status.innerText = '❌ Error: CORS o Formato no soportado.';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function drawGraph(data, threshold) {\n" +
                "            const svg = document.getElementById('graphSvg');\n" +
                "            svg.innerHTML = '';\n" +
                "            \n" +
                "            const w = svg.clientWidth;\n" +
                "            const h = svg.clientHeight;\n" +
                "            const maxScore = 5000;\n" +
                "            \n" +
                "            let pathD = `M 0 ${h} `;\n" +
                "            let alertZones = '';\n" +
                "\n" +
                "            data.forEach((pt, i) => {\n" +
                "                const x = (i / data.length) * w;\n" +
                "                const y = h - (Math.min(pt.s, maxScore) / maxScore) * h;\n" +
                "                pathD += `L ${x} ${y} `;\n" +
                "\n" +
                "                if (pt.s > threshold) {\n" +
                "                    alertZones += `<rect x=\"${x}\" y=\"0\" width=\"${w/data.length}\" height=\"${h}\" fill=\"#da3633\" opacity=\"0.2\" />`;\n" +
                "                }\n" +
                "            });\n" +
                "            \n" +
                "            pathD += `L ${w} ${h} Z`;\n" +
                "            const thY = h - (Math.min(threshold, maxScore) / maxScore) * h;\n" +
                "            \n" +
                "            svg.innerHTML += alertZones;\n" +
                "            svg.innerHTML += `<path d=\"${pathD}\" class=\"graph-line\" />`;\n" +
                "            svg.innerHTML += `<line x1=\"0\" y1=\"${thY}\" x2=\"${w}\" y2=\"${thY}\" class=\"graph-threshold\" />`;\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
