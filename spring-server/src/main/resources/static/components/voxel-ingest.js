/**
 * Full voxel ingest component: three folds per isometric face.
 * Fold 1 image, Fold 2 image+mask + PixelLight brush grid (square = texture = brush),
 * Fold 3 pixelated grid + acute slim neighbor faces. Default: complete 6 sides.
 * Edit mode: Paint (palette-chosen only) and Select (inspect, does not choose paint).
 */
(function (global) {
  const FACES = ["up", "down", "north", "south", "east", "west"];
  const PRIMARIES = ["#ff0000", "#00ff00", "#0000ff"];

  function hex(n) {
    return "#" + (n >>> 0).toString(16).padStart(8, "0").slice(2);
  }

  function rgbToHex(r, g, b) {
    return hex((r << 16) | (g << 8) | b);
  }

  function complement(h) {
    const n = parseInt(h.slice(1), 16);
    const r = 255 - ((n >> 16) & 255);
    const g = 255 - ((n >> 8) & 255);
    const b = 255 - (n & 255);
    return rgbToHex(r, g, b);
  }

  function pixelate(img, grid) {
    const c = document.createElement("canvas");
    c.width = grid;
    c.height = grid;
    const g = c.getContext("2d");
    g.imageSmoothingEnabled = false;
    g.drawImage(img, 0, 0, grid, grid);
    return g.getImageData(0, 0, grid, grid);
  }

  function cellIndex(u, v, grid) {
    return (v * grid + u) * 4;
  }

  function cellHex(data, u, v, grid) {
    const i = cellIndex(u, v, grid);
    const d = data.data;
    if (d[i + 3] <= 16) {
      return null;
    }
    return rgbToHex(d[i], d[i + 1], d[i + 2]);
  }

  function activeColors(data, grid) {
    const out = [];
    const seen = new Set();
    for (let v = 0; v < grid; v++) {
      for (let u = 0; u < grid; u++) {
        const h = cellHex(data, u, v, grid);
        if (h && !seen.has(h)) {
          seen.add(h);
          out.push(h);
        }
      }
    }
    return out;
  }

  function buildPalette(data, grid) {
    const active = activeColors(data, grid);
    const used = new Set(active.concat(PRIMARIES));
    const primaryComp = PRIMARIES.map(complement);
    primaryComp.forEach((h) => used.add(h));
    const activeComp = [];
    active.forEach((h) => {
      const c = complement(h);
      if (!used.has(c)) {
        used.add(c);
        activeComp.push(c);
      }
    });
    return {
      active: active,
      primary: PRIMARIES.slice(),
      primaryComp: primaryComp,
      activeComp: activeComp,
    };
  }

  function addressOf(artworkId, face, u, v, t) {
    return artworkId + ":" + face + ":" + u + ":" + v + (t == null ? "" : ":t=" + t);
  }

  function renderGrid(el, imageData, grid, onCell) {
    el.innerHTML = "";
    el.className = "grid";
    const d = imageData.data;
    for (let v = 0; v < grid; v++) {
      for (let u = 0; u < grid; u++) {
        const i = cellIndex(u, v, grid);
        const cell = document.createElement("div");
        const h = d[i + 3] <= 16 ? "" : rgbToHex(d[i], d[i + 1], d[i + 2]);
        cell.className = "cell";
        cell.dataset.u = String(u);
        cell.dataset.v = String(v);
        cell.dataset.hex = h;
        cell.style.background = "rgb(" + d[i] + "," + d[i + 1] + "," + d[i + 2] + ")";
        cell.title = h ? "brush " + h : "unmasked";
        cell.onclick = () => {
          el.dispatchEvent(new CustomEvent("brush-square", { detail: { u: u, v: v, brush: cell.title } }));
          if (onCell) {
            onCell(u, v, h || rgbToHex(d[i], d[i + 1], d[i + 2]));
          }
        };
        el.appendChild(cell);
      }
    }
  }

  function mount(host, opts) {
    opts = opts || {};
    const grid = opts.grid || 16;
    const artworkId = opts.artworkId || "demo";
    let face = opts.face || "north";
    const t = opts.t;
    let imageData = null;
    let editing = false;
    let tool = "paint";
    let paintColor = null;
    let inspectHex = null;

    host.innerHTML = "";
    const wrap = document.createElement("div");
    wrap.className = "voxel-ingest";
    wrap.innerHTML =
      '<div class="fold" data-fold="1"><h3>Fold 1 — image</h3><canvas class="src"></canvas></div>' +
      '<div class="fold" data-fold="2"><h3>Fold 2 — image + mask (square = texture = brush)</h3><div class="mask-grid"></div></div>' +
      '<div class="fold" data-fold="3"><h3>Fold 3 — PixelLight grid <button type="button" class="pl-edit">Edit</button></h3>' +
      '<div class="slim slim-up" title="up"></div>' +
      '<div class="pixellight-grid"></div>' +
      '<div class="slim slim-down" title="down"></div>' +
      '<p class="neighbors">slim neighbors: <span class="nb"></span></p></div>' +
      '<div class="pl-editor" hidden>' +
      '<div class="pl-tools">' +
      '<button type="button" class="pl-tool on" data-tool="paint">Paint</button>' +
      '<button type="button" class="pl-tool" data-tool="select">Select</button>' +
      "</div>" +
      '<div class="pl-palette"></div>' +
      '<p class="pl-info wa-meta">Choose a palette color to paint, or Select a square to inspect.</p>' +
      "</div>" +
      '<p class="faces">faces: <span class="checklist"></span></p>';
    host.appendChild(wrap);

    const editor = wrap.querySelector(".pl-editor");
    const paletteEl = wrap.querySelector(".pl-palette");
    const infoEl = wrap.querySelector(".pl-info");
    const editBtn = wrap.querySelector(".pl-edit");

    function checklist() {
      wrap.querySelector(".checklist").textContent = FACES.join(" · ") + " (current " + face + ")";
      wrap.querySelector(".nb").textContent = FACES.filter((f) => f !== face).join(", ");
    }
    checklist();

    function setInfo(text) {
      infoEl.textContent = text;
    }

    function applyHighlights() {
      wrap.querySelectorAll(".cell").forEach((cell) => {
        cell.classList.toggle("inspect", !!(inspectHex && cell.dataset.hex === inspectHex));
      });
      paletteEl.querySelectorAll(".pl-swatch").forEach((sw) => {
        sw.classList.toggle("chosen", !!(paintColor && sw.dataset.hex === paintColor));
        sw.classList.toggle("inspect", !!(inspectHex && sw.dataset.hex === inspectHex && sw.dataset.group === "active"));
      });
    }

    function renderPalette() {
      paletteEl.innerHTML = "";
      if (!imageData) {
        return;
      }
      const pal = buildPalette(imageData, grid);
      const groups = [
        ["active", "Active", pal.active],
        ["primary", "Primary", pal.primary],
        ["primaryComp", "Complements of primary", pal.primaryComp],
        ["activeComp", "Complements of active", pal.activeComp],
      ];
      groups.forEach((g) => {
        const row = document.createElement("div");
        row.className = "pl-group";
        const lab = document.createElement("span");
        lab.className = "pl-group-label";
        lab.textContent = g[1];
        row.appendChild(lab);
        g[2].forEach((h) => {
          const sw = document.createElement("button");
          sw.type = "button";
          sw.className = "pl-swatch";
          sw.dataset.hex = h;
          sw.dataset.group = g[0];
          sw.style.background = h;
          sw.title = h;
          sw.onclick = () => {
            if (tool === "paint") {
              paintColor = h;
              setInfo("paint " + h);
              applyHighlights();
            } else {
              inspectHex = h;
              setInfo("select " + h + " (not chosen as paint)");
              applyHighlights();
            }
          };
          row.appendChild(sw);
        });
        paletteEl.appendChild(row);
      });
      applyHighlights();
    }

    function paintCell(u, v) {
      if (!imageData || !paintColor) {
        setInfo("choose a palette color to paint");
        return;
      }
      const n = parseInt(paintColor.slice(1), 16);
      const i = cellIndex(u, v, grid);
      imageData.data[i] = (n >> 16) & 255;
      imageData.data[i + 1] = (n >> 8) & 255;
      imageData.data[i + 2] = n & 255;
      imageData.data[i + 3] = 255;
      drawGrids();
      renderPalette();
      const addr = addressOf(artworkId, face, u, v, t);
      setInfo("painted " + paintColor + " · " + addr);
      const body = { artworkId: artworkId, face: face, u: u, v: v, colorHex: paintColor };
      if (t != null) {
        body.t = t;
      }
      fetch("/api/voxel/stamp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      }).catch(() => {});
    }

    function selectCell(u, v) {
      if (!imageData) {
        return;
      }
      const i = cellIndex(u, v, grid);
      const d = imageData.data;
      const h = rgbToHex(d[i], d[i + 1], d[i + 2]);
      inspectHex = h;
      const addr = addressOf(artworkId, face, u, v, t);
      setInfo("select " + h + " · rgb(" + d[i] + "," + d[i + 1] + "," + d[i + 2] + ") · u=" + u + " v=" + v + " · " + addr + " (not chosen as paint)");
      applyHighlights();
    }

    function onCell(u, v) {
      if (!editing) {
        return;
      }
      if (tool === "select") {
        selectCell(u, v);
        return;
      }
      paintCell(u, v);
    }

    function drawGrids() {
      if (!imageData) {
        return;
      }
      renderGrid(wrap.querySelector(".mask-grid"), imageData, grid, onCell);
      renderGrid(wrap.querySelector(".pixellight-grid"), imageData, grid, onCell);
      applyHighlights();
    }

    function applyImage(img) {
      const src = wrap.querySelector(".src");
      src.width = 160;
      src.height = 160;
      src.getContext("2d").drawImage(img, 0, 0, 160, 160);
      imageData = pixelate(img, grid);
      drawGrids();
      renderPalette();
      FACES.filter((f) => f !== face).forEach((f, i) => {
        const slim = wrap.querySelector(i === 0 ? ".slim-up" : ".slim-down") || wrap.querySelector(".slim-up");
        slim.style.background = "linear-gradient(90deg,#444,#888)";
        slim.onclick = () => {
          face = f;
          checklist();
          applyImage(img);
        };
      });
      wrap.dataset.address = addressOf(artworkId, face, 0, 0, t);
    }

    editBtn.onclick = () => {
      editing = !editing;
      editor.hidden = !editing;
      editBtn.classList.toggle("on", editing);
      editBtn.textContent = editing ? "Done" : "Edit";
      if (!editing) {
        inspectHex = null;
        applyHighlights();
        setInfo("Choose a palette color to paint, or Select a square to inspect.");
      }
    };

    wrap.querySelectorAll(".pl-tool").forEach((btn) => {
      btn.onclick = () => {
        tool = btn.dataset.tool;
        wrap.querySelectorAll(".pl-tool").forEach((b) => b.classList.toggle("on", b === btn));
        if (tool === "paint") {
          setInfo(paintColor ? "paint " + paintColor : "choose a palette color to paint");
        } else {
          setInfo("Select a square to inspect color (does not choose paint)");
        }
      };
    });

    if (opts.imageUrl) {
      const img = new Image();
      img.onload = () => applyImage(img);
      img.src = opts.imageUrl;
    }
    if (opts.image) {
      applyImage(opts.image);
    }
    return { applyImage, address: () => wrap.dataset.address };
  }

  global.VoxelIngest = { mount, FACES, complement: complement, buildPalette: buildPalette };
})(window);
