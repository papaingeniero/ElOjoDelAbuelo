package com.elojodelabuelo;
public class WebOsdEditor { 
    public static String getHtml() { 
        // Recuperamos los valores actuales para inicializar el JS 
        float xPct = SentinelService.OSD_X_PCT; 
        float yPct = SentinelService.OSD_Y_PCT;

    return "<html><head><meta charset='UTF-8'><title>OSD Config</title>" +
        "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
        "<style>" +
        "body { background:#111; color:#0f0; font-family:monospace; text-align:center; padding:20px; }" +
        ".preview-area { position:relative; width:100%; max-width:480px; aspect-ratio:4/3; background:#000; border:2px dashed #333; margin:20px auto; overflow:hidden; }" +
        ".osd-label { position:absolute; padding:5px; border:1px solid #0f0; background:rgba(0,255,0,0.1); cursor:move; font-weight:bold; user-select:none; white-space:nowrap; }" +
        ".btn { background:#050; color:#fff; border:1px solid #0f0; padding:12px 24px; cursor:pointer; font-weight:bold; margin-top:20px; }" +
        "</style></head><body>" +
        "<h2>📟 POSICIONADOR OSD</h2>" +
        "<div class='preview-area' id='canvas'>" +
        "<div id='drag' class='osd-label'>12/05/2026 23:59:59</div>" +
        "</div>" +
        "<button class='btn' onclick='save()'>💾 GUARDAR EN EL ABUELO</button>" +
        "<button class='btn' style='background:#333; margin-left:10px' onclick='window.close()'>CERRAR</button>" +
        "<script>" +
        "var d=document.getElementById('drag'), c=document.getElementById('canvas'), active=false, curX, curY, initX, initY;" +
        "var xPct=" + xPct + ", yPct=" + yPct + ";" +
        "function setup(){ " +
        "  d.style.left=(xPct*100)+'%'; d.style.top=(yPct*100)+'%'; " +
        "}" +
        "setup();" +
        "c.addEventListener('touchstart',s,false); c.addEventListener('touchmove',m,false); c.addEventListener('touchend',e,false);" +
        "c.addEventListener('mousedown',s,false); c.addEventListener('mousemove',m,false); window.addEventListener('mouseup',e,false);" +
        "function s(ev){ if(ev.target===d){ active=true; var t=ev.touches?ev.touches[0]:ev; initX=t.clientX-d.offsetLeft; initY=t.clientY-d.offsetTop; } }" +
        "function e(){ active=false; }" +
        "function m(ev){ if(active){ ev.preventDefault(); var t=ev.touches?ev.touches[0]:ev; var x=t.clientX-initX, y=t.clientY-initY;" +
        "d.style.left=Math.max(0,Math.min(x,c.offsetWidth-d.offsetWidth))+'px';" +
        "d.style.top=Math.max(0,Math.min(y,c.offsetHeight-d.offsetHeight))+'px'; } }" +
        "function save(){ " +
        " var xFinal = d.offsetLeft / c.offsetWidth; var yFinal = d.offsetTop / c.offsetHeight;" +
        " fetch('/api/set_osd?x='+xFinal+'&y='+yFinal).then(r=>r.text()).then(t=>{ alert(t==='OK'?'✅ Posición actualizada':'❌ Error'); });" +
        "}" +
        "</script></body></html>";
}
}
