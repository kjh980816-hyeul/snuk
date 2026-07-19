// ════════════════════════════════════════════
// SNUK 시안 렌더러 — 실데이터 구동
// 데이터: window.__SNUK_DATA (SnukShell.vue 가 실제 API 로 구성)
// 액션 : window.__snukActions (신청/후기/스포트라이트/굿즈 — Vue 측 실 API 호출)
// 더미 데이터 금지 — 데이터 없으면 empty state.
// ════════════════════════════════════════════
const D = () => window.__SNUK_DATA || {};
const A = () => window.__snukActions || {};

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
));

const PLACEHOLDER_BG = ['linear-gradient(135deg,#1a1040,#2d1060)', 'linear-gradient(135deg,#0a2040,#0a4080)', 'linear-gradient(135deg,#401010,#801020)', 'linear-gradient(135deg,#0a3020,#0a6040)', 'linear-gradient(135deg,#302010,#604020)', 'linear-gradient(135deg,#201040,#401080)'];
const bgOf = (i) => PLACEHOLDER_BG[i % PLACEHOLDER_BG.length];

function emptyCard(msg) {
  return `<div style="width:100%;border:1px dashed var(--border2);border-radius:12px;padding:36px 16px;text-align:center;color:var(--text3);font-size:13px;">${esc(msg)}</div>`;
}

function thumbHtml(img, i, emoji) {
  if (img) {
    return `<img src="${esc(img)}" alt="" style="position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:top;" onerror="this.remove()">`;
  }
  return `<div class="card-thumb-placeholder">${emoji || '◆'}</div>`;
}

// ════════════════════════════════════════════
// CARD WIDTH HELPER
// ════════════════════════════════════════════
function cardWidth(n, mobileN) {
  const w = window.innerWidth;
  // 모바일에서 카드가 좁아지면 뱃지/버튼이 세로로 꺾이므로 카드별로 모바일 열수를 지정 가능
  if (w <= 480) n = Math.min(n, mobileN ?? 2);
  else if (w <= 1024) n = Math.min(n, 3);
  return `calc((100% - ${Math.max(0, n - 1) * 16}px) / ${n})`;
}

// ════════════════════════════════════════════
// 컨텐츠/대회 카드 (실데이터: SnukCard)
// ════════════════════════════════════════════
function makeContentCard(d, w, i) {
  const canApply = d.status === 'open';
  const badgeCls = d.status === 'open' ? 'open' : d.status === 'ongoing' ? 'ongoing' : 'closed';
  const badge = `<span class="badge ${badgeCls}">${esc(d.statusLabel)}</span>`;
  const slots = d.max > 0 ? `모집 ${d.filled}/${d.max}명` : '';
  return `<div class="content-card" style="width:${w};min-width:${w};cursor:pointer;" ${d.kind === 'tournament' ? `onclick="__snukNav('/championship/${d.id}')"` : canApply ? `onclick="openApply('${d.kind}',${d.id})"` : ''}>
    <div class="card-thumb" style="background:${bgOf(i)};position:relative;">
      ${thumbHtml(d.img, i)}
      <span class="big-kind" style="position:absolute;top:8px;right:8px;">${d.kind === 'tournament' ? '대회' : '컨텐츠'}</span>
    </div>
    <div class="card-body">
      <div class="card-meta">${badge}${d.eventDate ? `<span style="font-size:11px;color:var(--text3);margin-left:auto;">${esc(d.eventDate)}</span>` : ''}</div>
      <div class="card-title">${esc(d.title)}</div>
      <div class="card-desc">${esc(d.desc)}</div>
      <div class="card-footer">
        <div class="card-members">${slots}</div>
        ${canApply ? `<button class="btn-apply" onclick="event.stopPropagation();openApply('${d.kind}',${d.id})">신청하기</button>` : ''}
        ${d.kind === 'tournament' && d.resultText ? `<button class="btn btn-outline" style="padding:6px 12px;font-size:11px;" onclick="event.stopPropagation();showResult(${d.id})">결과 보기</button>` : ''}
      </div>
    </div>
  </div>`;
}

function initSlider(id, cards) { const el = document.getElementById(id); if (el) el.innerHTML = cards; }

function renderContentSliders() {
  const snuk = D().snukContents || [];
  const mug = D().mugContents || [];
  // 컨텐츠·대회 통합 목록(/campaigns) — 모집중 우선
  const merged = [...snuk, ...mug]
    .sort((a, b) => (a.status === 'open' ? 0 : 1) - (b.status === 'open' ? 0 : 1));
  initSlider('snuk-slider', merged.length
    ? merged.map((d, i) => makeContentCard(d, cardWidth(5, 1), i)).join('')
    : emptyCard('진행 중인 컨텐츠가 없습니다. 곧 새로운 컨텐츠로 찾아올게요!'));
  initSlider('mug-slider', mug.length
    ? mug.map((d, i) => makeContentCard(d, cardWidth(5, 1), i)).join('')
    : emptyCard('등록된 대회가 없습니다.'));
  // 스트리머 컨텐츠/이벤트/구인구직 — 데이터 소스 없음(향후 오픈)
  initSlider('str-slider', emptyCard('스트리머 컨텐츠는 준비 중입니다.'));
  initSlider('event-slider', emptyCard('진행 중인 이벤트가 없습니다.'));
  initSlider('job-slider', emptyCard('등록된 공고가 없습니다.'));
}

// ════════════════════════════════════════════
// 홈 통합 빅그리드 — 캠페인 + 대회 큰 카드 (3열 슬라이드)
// ════════════════════════════════════════════
function makeBigCard(d, w, i) {
  const canApply = d.status === 'open';
  const badgeCls = d.status === 'open' ? 'open' : d.status === 'ongoing' ? 'ongoing' : 'closed';
  const kindLabel = d.kind === 'tournament' ? '대회' : '컨텐츠';
  const slots = d.max > 0 ? `모집 ${d.filled}/${d.max}명` : '';
  const sub = [slots, d.eventDate].filter(Boolean).join(' · ');
  return `<div class="content-card big-card" style="width:${w};min-width:${w};cursor:pointer;" ${d.kind === 'tournament' ? `onclick="__snukNav('/championship/${d.id}')"` : canApply ? `onclick="openApply('${d.kind}',${d.id})"` : `onclick="window.__snukNav('/campaigns')"`}>
    <div class="card-thumb" style="background:${bgOf(i)};position:relative;">
      ${thumbHtml(d.img, i, d.kind === 'tournament' ? '🏆' : '🎮')}
      <div class="big-card-grad"></div>
      <div class="big-card-top">
        <span class="badge ${badgeCls}">${esc(d.statusLabel)}</span>
        <span class="big-kind">${kindLabel}</span>
      </div>
      <div class="big-card-caption">
        <div class="big-title">${esc(d.title)}</div>
        ${sub ? `<div class="big-sub">${esc(sub)}</div>` : ''}
      </div>
    </div>
    <div class="big-card-foot">
      <div class="card-desc">${esc(d.desc)}</div>
      ${canApply ? `<button class="btn-apply" onclick="event.stopPropagation();openApply('${d.kind}',${d.id})">신청하기</button>`
        : d.kind === 'tournament' && d.resultText ? `<button class="btn btn-outline" style="padding:6px 12px;font-size:11px;flex-shrink:0;" onclick="event.stopPropagation();showResult(${d.id})">결과 보기</button>`
        : `<span style="font-size:11px;color:var(--text3);flex-shrink:0;">${esc(d.statusLabel)}</span>`}
    </div>
  </div>`;
}

function initBigContents() {
  // 홈: 큰 피처드 카드 + 옆 작은 부가 카드 (항목 3/5) + 대회 그리드
  initHomeFeature();
  fillBigTrack('big-tour-slider', D().mugContents || [], '예정된 대회가 없습니다. 새 대회 소식을 기다려주세요!');
}

// ── 자동 슬라이드 타이머 공용(멱등 init 대비 키별 1개 유지, 호버 시 일시정지)
const _autoTimers = {};
function setAutoLoop(key, fn, ms) {
  if (_autoTimers[key]) clearInterval(_autoTimers[key]);
  _autoTimers[key] = setInterval(fn, ms);
}
function hoverPaused(el) {
  try { return !!(el && el.closest('section') && el.closest('section').matches(':hover')); } catch { return false; }
}

// ── 홈 피처드 레이아웃: 큰 칸 1 + 작은 부가 카드
// 큰 칸 = 어드민이 체크(featured)한 컨텐츠. 미선택이면 모집중 우선 상위 5개 자동 순환.
let _featureIdx = 0;
function makeFeatureMainCard(d, i, dotsHtml) {
  const canApply = d.status === 'open';
  const badgeCls = d.status === 'open' ? 'open' : d.status === 'ongoing' ? 'ongoing' : 'closed';
  const slots = d.max > 0 ? `모집 ${d.filled}/${d.max}명` : '';
  const sub = [slots, d.eventDate].filter(Boolean).join(' · ');
  const click = d.kind === 'tournament' ? `__snukNav('/championship/${d.id}')`
    : canApply ? `openApply('${d.kind}',${d.id})` : `window.__snukNav('/campaigns')`;
  return `<div class="feature-main-card" onclick="${click}" style="background:${bgOf(i)};">
    ${d.img ? `<img src="${esc(d.img)}" alt="" onerror="this.remove()">` : `<div class="feature-main-emoji">🎮</div>`}
    <div class="feature-main-grad"></div>
    <div class="feature-main-top">
      <span class="badge ${badgeCls}">${esc(d.statusLabel)}</span>
      <span class="big-kind">${d.kind === 'tournament' ? '대회' : '컨텐츠'}</span>
    </div>
    <div class="feature-main-caption">
      <div class="feature-main-title">${esc(d.title)}</div>
      ${d.desc ? `<div class="feature-main-desc">${esc(d.desc)}</div>` : ''}
      <div class="feature-main-foot">
        ${sub ? `<span class="feature-main-sub">${esc(sub)}</span>` : '<span></span>'}
        ${canApply ? `<button class="btn-apply" onclick="event.stopPropagation();openApply('${d.kind}',${d.id})">신청하기</button>` : ''}
      </div>
      ${dotsHtml || ''}
    </div>
  </div>`;
}

function makeFeatureSideCard(d, i) {
  const badgeCls = d.status === 'open' ? 'open' : d.status === 'ongoing' ? 'ongoing' : 'closed';
  const click = d.kind === 'tournament' ? `__snukNav('/championship/${d.id}')`
    : d.status === 'open' ? `openApply('${d.kind}',${d.id})` : `window.__snukNav('/campaigns')`;
  return `<div class="feature-side-card" onclick="${click}">
    <div class="feature-side-thumb" style="background:${bgOf(i + 2)};">
      ${d.img ? `<img src="${esc(d.img)}" alt="" onerror="this.remove()">` : ''}
      <span class="badge ${badgeCls}" style="position:absolute;top:8px;left:8px;">${esc(d.statusLabel)}</span>
      <span class="big-kind" style="position:absolute;top:8px;right:8px;">${d.kind === 'tournament' ? '대회' : '컨텐츠'}</span>
    </div>
    <div class="feature-side-body">
      <div class="feature-side-title">${esc(d.title)}</div>
      <div class="feature-side-sub">${d.max > 0 ? `모집 ${d.filled}/${d.max}명` : esc(d.statusLabel)}</div>
    </div>
  </div>`;
}

// 컨텐츠+대회 통합 섹션 — 큰 칸=스눅 공식(관리자 등록) 컨텐츠 전용, 나머지(대회·스트리머 컨텐츠)는 작은 카드
function initHomeFeature() {
  const main = document.getElementById('home-feature-main');
  const sideL = document.getElementById('home-feature-side-l');
  const sideR = document.getElementById('home-feature-side-r');
  if (!main || !sideL || !sideR) return;
  const openFirst = (arr) => [...arr].sort((a, b) => (a.status === 'open' ? 0 : 1) - (b.status === 'open' ? 0 : 1));
  const campaigns = D().snukContents || [];
  const tournaments = D().mugContents || [];
  // 큰 칸 후보 = 스눅 공식(관리자 등록 컨텐츠·대회)만 — 스트리머 등록분은 작은 카드로만
  const adminPool = openFirst([...campaigns, ...tournaments].filter((x) => x.adminMade !== false));
  const everything = openFirst([...campaigns, ...tournaments]);                // 작은 칸 = 컨텐츠+대회 전부
  if (_autoTimers.homeFeature) { clearInterval(_autoTimers.homeFeature); _autoTimers.homeFeature = null; }
  if (!campaigns.length && !tournaments.length) {
    main.innerHTML = emptyCard('진행 중인 컨텐츠가 없습니다. 곧 새로운 컨텐츠로 찾아올게요!');
    sideL.innerHTML = '';
    sideR.innerHTML = '';
    return;
  }
  // 큰 칸을 가운데 두고 작은 카드를 좌 2 / 우 2 로 배치
  const renderSide = (excl) => {
    const rest = everything
      .filter((x) => !(excl && x.kind === excl.kind && x.id === excl.id))
      .slice(0, 4);
    sideL.innerHTML = rest.slice(0, 2).map((d, i) => makeFeatureSideCard(d, i)).join('');
    sideR.innerHTML = rest.slice(2, 4).map((d, i) => makeFeatureSideCard(d, i + 2)).join('');
  };
  if (!adminPool.length) {
    main.innerHTML = emptyCard('SNUK 공식 컨텐츠 준비 중입니다.');
    renderSide(null);
    return;
  }
  const featured = D().snukFeatured || D().mugFeatured; // featured 는 어드민 폼에서만 체크 가능 = 항상 공식(컨텐츠 우선, 없으면 대회)
  if (featured) {
    main.innerHTML = makeFeatureMainCard(featured, 0, '');
    renderSide(featured);
    return;
  }
  // 미지정 — 공식 컨텐츠 상위 5개 자동 슬라이드(4초, 호버 시 정지)
  const rotation = adminPool.slice(0, 5);
  _featureIdx = _featureIdx % rotation.length;
  const dots = () => `<div class="feature-dots">${rotation.map((_, i) =>
    `<span class="${i === _featureIdx ? 'on' : ''}"></span>`).join('')}</div>`;
  const paint = () => { main.innerHTML = makeFeatureMainCard(rotation[_featureIdx], _featureIdx, dots()); };
  paint();
  renderSide(rotation[_featureIdx]);
  setAutoLoop('homeFeature', () => {
    if (!document.getElementById('home-feature-main')) return;
    if (hoverPaused(main)) return;
    _featureIdx = (_featureIdx + 1) % rotation.length;
    paint();
  }, 4000);
}

// ── 라이브 배너 (히어로 아래, 어드민 on/off — 항목 13/18)
function initLiveBanner() {
  const sec = document.getElementById('live-banner');
  if (!sec) return;
  const ss = (D() && D().siteSettings) || {};
  const url = (ss.LIVE_BANNER_URL || '').trim();
  const on = ss.LIVE_BANNER_ENABLED === '1' && url && url !== '-';
  sec.style.display = on ? '' : 'none';
  const wrap = document.getElementById('live-embed-wrap');
  if (!on) { if (wrap) wrap.style.display = 'none'; return; }
  const titleEl = document.getElementById('live-banner-title');
  const title = (ss.LIVE_BANNER_TITLE || '').trim();
  if (titleEl) titleEl.textContent = (title && title !== '-') ? title : '지금 방송 중';
  // 방송중이면 배너 밑에 치지직 플레이어+채팅 임베드 (오프라인이면 배너만)
  const ch = (ss.LIVE_CHANNEL_ID || '').trim();
  if (!wrap || !ch || ch === '-') { if (wrap) wrap.style.display = 'none'; return; }
  fetch('/api/live/status').then((r) => r.json()).then((s) => {
    if (!s.live) { wrap.style.display = 'none'; return; }
    wrap.style.display = '';
    const pl = document.getElementById('live-embed-player');
    const chat = document.getElementById('live-embed-chat');
    if (pl && !pl.src.includes(ch)) pl.src = `https://chzzk.naver.com/live/${ch}`;
    if (chat && !chat.src.includes(ch)) chat.src = `https://chzzk.naver.com/live/${ch}/chat`;
    fitLivePlayerCrop();
    if (!window.__liveCropBound) {
      window.__liveCropBound = true;
      window.addEventListener('resize', fitLivePlayerCrop);
    }
    // 어드민 제목 미설정 시 실제 방송 제목 표시
    if (titleEl && !(title && title !== '-') && s.liveTitle) titleEl.textContent = s.liveTitle;
  }).catch(() => { wrap.style.display = 'none'; });
}
// 크롭 스케일: 컨테이너 폭에 맞춰 치지직 페이지(1620 로드)의 영상 영역(x240,y60,1026 폭)만 보이게
function fitLivePlayerCrop() {
  const crop = document.getElementById('live-embed-player-crop');
  const pl = document.getElementById('live-embed-player');
  if (!crop || !pl || !crop.clientWidth) return;
  const s = crop.clientWidth / 1026;
  pl.style.transform = `scale(${s}) translate(-240px, -60px)`;
}

function openLiveBanner() {
  const ss = (D() && D().siteSettings) || {};
  const url = (ss.LIVE_BANNER_URL || '').trim();
  if (url && url !== '-') window.open(url, '_blank');
}

// ── SNUK 뉴스 (홈 섹션 — 항목 11)
function initNews() {
  const grid = document.getElementById('news-grid');
  if (!grid) return;
  const news = D().news || [];
  if (!news.length) {
    grid.innerHTML = emptyCard('아직 등록된 기사가 없습니다.');
    return;
  }
  grid.innerHTML = news.slice(0, 4).map((n, i) => `
    <div class="news-card" onclick="window.__snukNav('/news/${n.id}')">
      <div class="news-card-thumb" style="background:${bgOf(i + 1)};">
        ${n.thumb ? `<img src="${esc(n.thumb)}" alt="" onerror="this.remove()">` : '<div class="news-card-emoji">📰</div>'}
      </div>
      <div class="news-card-body">
        <div class="news-card-title">${esc(n.title)}</div>
        ${n.excerpt ? `<div class="news-card-excerpt">${esc(n.excerpt)}</div>` : ''}
        <div class="news-card-meta">${esc(n.author)} · ${esc(n.date)}</div>
      </div>
    </div>`).join('');
}

function fillBigTrack(trackId, items, emptyMsg) {
  const track = document.getElementById(trackId);
  if (!track) return;
  if (!items.length) {
    track.innerHTML = emptyCard(emptyMsg);
    return;
  }
  const sorted = [...items].sort((a, b) => (a.status === 'open' ? 0 : 1) - (b.status === 'open' ? 0 : 1));
  const w = cardWidth(3, 1);
  track.innerHTML = sorted.map((d, i) => makeBigCard(d, w, i)).join('');
}

// ── FEATURED 카드 (SNUK 컨텐츠/챔피언십 상단)
function renderFeatured(elId, d, tagText) {
  const el = document.getElementById(elId);
  if (!el) return;
  if (!d) { el.style.display = 'none'; return; }
  const canApply = d.status === 'open';
  const badgeCls = d.status === 'open' ? 'open' : d.status === 'ongoing' ? 'ongoing' : 'closed';
  el.style.display = '';
  el.setAttribute('onclick', d.kind === 'tournament' ? `__snukNav('/championship/${d.id}')` : canApply ? `openApply('${d.kind}',${d.id})` : '');
  el.innerHTML = `
    <div class="featured-thumb" style="background:linear-gradient(135deg,#1a1040,#2d1060);position:relative;">
      ${d.img ? `<img src="${esc(d.img)}" alt="" style="position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:top;" onerror="this.remove()">` : '<div style="font-size:56px;opacity:.5;">🎮</div>'}
      <div style="position:absolute;top:14px;left:14px;"><span class="badge ${badgeCls}">${esc(d.statusLabel)}</span></div>
    </div>
    <div class="featured-body">
      <div class="featured-tag">${tagText}</div>
      <h3 class="featured-title">${esc(d.title)}</h3>
      <p class="featured-desc">${esc(d.desc)}</p>
      <div class="featured-stats">
        ${d.max > 0 ? `<div class="featured-stat"><strong>${d.filled}/${d.max}명</strong>모집 현황</div>` : ''}
        ${d.eventDate ? `<div class="featured-stat"><strong>${esc(d.eventDate)}</strong>진행일</div>` : ''}
        <div class="featured-stat"><strong>${esc(d.statusLabel)}</strong>현재 상태</div>
      </div>
      <div style="display:flex;align-items:center;gap:8px;">
        ${canApply ? `<button class="btn-apply" onclick="event.stopPropagation();openApply('${d.kind}',${d.id})">신청하기</button>`
          : `<button class="btn-apply" style="background:var(--bg4);color:var(--text3);cursor:default;" onclick="event.stopPropagation()">${esc(d.statusLabel)}</button>`}
        ${d.kind === 'tournament' && d.resultText ? `<button class="btn btn-outline" onclick="event.stopPropagation();showResult(${d.id})">결과 보기</button>` : ''}
      </div>
    </div>`;
}

// ── 대회 결과 모달(동적 생성)
function showResult(tournamentId) {
  const t = (D().mugContents || []).find((x) => x.id === tournamentId);
  if (!t || !t.resultText) return;
  openDynamicModal(`<div class="modal-title">${esc(t.title)}</div>
    <div class="modal-sub">대회 결과</div>
    <div style="background:var(--bg3);border-radius:10px;padding:16px;font-size:13px;color:var(--text);line-height:1.8;white-space:pre-wrap;">${esc(t.resultText)}</div>`);
}

// ── 동적 모달(공지/결과 공용)
function openDynamicModal(innerHtml, width) {
  let ov = document.getElementById('snuk-dyn-modal');
  if (!ov) {
    ov = document.createElement('div');
    ov.id = 'snuk-dyn-modal';
    ov.className = 'modal-overlay';
    ov.addEventListener('click', (e) => { if (e.target === ov) ov.classList.remove('open'); });
    (document.querySelector('.snuk-page') || document.body).appendChild(ov);
  }
  ov.innerHTML = `<div class="modal" style="width:${width || '480px'};max-height:80vh;overflow-y:auto;">
    <button class="modal-close" onclick="document.getElementById('snuk-dyn-modal').classList.remove('open')">✕</button>
    ${innerHtml}</div>`;
  ov.classList.add('open');
}

// ════════════════════════════════════════════
// 스트리머 컨텐츠·대회 등록 (STREAMER+ — 백엔드가 등급·소유자 재검증)
// 공식(featured·큰 칸)은 관리자 전용, 스트리머 등록분은 작은 카드로만 노출.
// ════════════════════════════════════════════
const SP_INP = 'width:100%;margin-bottom:8px;padding:10px 12px;background:var(--bg3);border:1px solid var(--border);border-radius:8px;color:var(--text);font-size:13px;font-family:inherit;box-sizing:border-box;';
let _spEditId = null;
let _spRaw = null; // 수정 시 원본(미노출 필드 보존용 — update 는 전필드 전송이라 유실 방지)

function initStreamerPost() {
  const me = window.__snukMe;
  const can = !!(me && (me.role === 'STREAMER' || me.role === 'REPORTER' || me.role === 'ADMIN'));
  [['snuk-contents', 'campaign', '+ 내 컨텐츠 등록'], ['mugchamps', 'tournament', '+ 내 대회 등록']].forEach(([sec, kind, label]) => {
    const header = document.querySelector(`#${sec} .section-header`);
    if (!header) return;
    let btn = header.querySelector('.streamer-post-btn');
    if (!can) { if (btn) btn.remove(); return; }
    if (!btn) {
      btn = document.createElement('button');
      btn.className = 'btn-apply streamer-post-btn';
      btn.style.marginLeft = 'auto';
      header.appendChild(btn);
    }
    btn.textContent = label;
    btn.onclick = () => openStreamerPost(kind);
  });
}
window.__snukInitStreamerPost = initStreamerPost;

function openStreamerPost(kind) {
  _spEditId = null;
  _spRaw = null;
  const me = window.__snukMe || {};
  const isT = kind === 'tournament';
  const mine = (isT ? (D().mugContents || []) : (D().snukContents || [])).filter((x) => x.ownerId === me.id);
  const rows = mine.map((x) => `
    <div style="display:flex;align-items:center;gap:8px;padding:8px 0;border-bottom:1px solid var(--border);">
      <span style="flex:1;min-width:0;font-size:13px;color:var(--text);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${esc(x.title)}</span>
      <span class="badge ${x.status === 'open' ? 'open' : 'closed'}">${esc(x.statusLabel)}</span>
      ${isT ? `<button class="btn btn-outline" style="font-size:11px;padding:5px 10px;color:#4cc38a;" onclick="spParticipants(${x.id},'${esc(x.title).replace(/'/g, '&#39;')}')">참가자</button>` : ''}
      <button class="btn btn-outline" style="font-size:11px;padding:5px 10px;" onclick="spEdit('${kind}',${x.id})">수정</button>
      <button class="btn btn-outline" style="font-size:11px;padding:5px 10px;color:#e5484d;" onclick="spDelete('${kind}',${x.id})">삭제</button>
    </div>`).join('');
  openDynamicModal(`
    <div class="modal-title">${isT ? '🏆 내 대회' : '🎮 내 컨텐츠'}</div>
    <div class="modal-sub">${isT ? '직접 주최하는 대회를 등록해보세요' : '"나 이런 거 할 건데 같이 할래?" — 함께할 스트리머를 모집해보세요'}</div>
    ${mine.length ? `<div style="margin-bottom:14px;">${rows}</div>` : ''}
    <input id="spc-title" style="${SP_INP}" placeholder="제목 *">
    <input id="spc-game" style="${SP_INP}" placeholder="게임명">
    <textarea id="spc-desc" style="${SP_INP}resize:vertical;" rows="3" placeholder="설명"></textarea>
    <div style="display:flex;gap:8px;">
      <input id="spc-date" type="date" style="${SP_INP}flex:1;" title="진행일">
      <input id="spc-cap" type="number" min="0" style="${SP_INP}flex:1;" placeholder="${isT ? '정원(명)' : '모집 인원'}">
      <select id="spc-status" style="${SP_INP}flex:1;">
        <option value="OPEN">모집중</option><option value="SCHEDULED">오픈예정</option><option value="CLOSED">마감</option>
      </select>
    </div>
    <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px;">
      <input id="spc-img" type="file" accept="image/*" style="font-size:12px;flex:1;color:var(--text2);">
      <img id="spc-img-prev" alt="" style="display:none;width:56px;height:36px;object-fit:cover;border-radius:6px;">
    </div>
    <button id="spc-submit" class="btn-apply" style="width:100%;padding:11px;" onclick="spSubmit('${kind}')">등록하기</button>`);
}

async function spEdit(kind, id) {
  try {
    const d = await A().getContent(kind, id);
    _spRaw = d;
    _spEditId = id;
    document.getElementById('spc-title').value = d.title || '';
    document.getElementById('spc-game').value = d.gameName || '';
    document.getElementById('spc-desc').value = d.description || '';
    document.getElementById('spc-date').value = d.eventDate || '';
    document.getElementById('spc-cap').value = kind === 'tournament' ? d.capacity : d.totalSlots;
    const sel = document.getElementById('spc-status');
    sel.value = ['OPEN', 'SCHEDULED', 'CLOSED'].includes(d.status) ? d.status : 'CLOSED';
    const img = d.promoImageUrl || d.bannerImageUrl;
    const prev = document.getElementById('spc-img-prev');
    if (img) { prev.src = img; prev.style.display = ''; }
    document.getElementById('spc-submit').textContent = '수정 저장';
    document.getElementById('spc-title').focus();
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '불러오기에 실패했습니다');
  }
}

// ── 주최자 참가자 관리 (본인 대회 신청자 승인/거절 — 백엔드가 소유자 재검증)
async function spParticipants(tourId, title) {
  let list;
  try {
    list = await A().manageParticipants(tourId);
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '불러오기에 실패했습니다');
    return;
  }
  const t = (D().mugContents || []).find((x) => x.id === tourId);
  const stLabel = { PENDING: '대기', APPROVED: '승인됨', REJECTED: '거절됨' };
  const stCls = { PENDING: '', APPROVED: 'open', REJECTED: 'closed' };
  const rows = list.map((p) => `
    <div style="display:flex;align-items:center;gap:10px;padding:9px 0;border-bottom:1px solid var(--border);">
      <div style="width:32px;height:32px;border-radius:50%;overflow:hidden;background:var(--bg3);flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:13px;color:var(--text2);">
        ${p.profileImageUrl ? `<img src="${esc(p.profileImageUrl)}" alt="" style="width:100%;height:100%;object-fit:cover;" onerror="this.remove()">` : esc((p.nickname || '?').slice(0, 1))}
      </div>
      <div style="flex:1;min-width:0;">
        <div style="font-size:13px;font-weight:600;color:var(--text);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${esc(p.nickname)}</div>
        <div style="font-size:11px;color:var(--text3);">팔로워 ${p.followerSnapshot.toLocaleString()}</div>
      </div>
      <span class="badge ${stCls[p.status] || ''}">${stLabel[p.status] || p.status}</span>
      ${p.status === 'PENDING' ? `
        <button class="btn btn-outline" style="font-size:11px;padding:5px 10px;color:#4cc38a;" onclick="spDecide(${tourId},${p.participantId},true,'${esc(title).replace(/'/g, '&#39;')}')">승인</button>
        <button class="btn btn-outline" style="font-size:11px;padding:5px 10px;color:#e5484d;" onclick="spDecide(${tourId},${p.participantId},false,'${esc(title).replace(/'/g, '&#39;')}')">거절</button>` : ''}
    </div>`).join('');
  const approved = list.filter((p) => p.status === 'APPROVED').length;
  openDynamicModal(`
    <div class="modal-title">👥 참가자 관리</div>
    <div class="modal-sub">${esc(title)} — 승인 ${approved}${t && t.max > 0 ? `/${t.max}` : ''}명 · 신청 ${list.length}건</div>
    ${rows || '<div style="font-size:13px;color:var(--text3);padding:16px 0;">아직 신청자가 없습니다.</div>'}
    <button class="btn btn-outline" style="width:100%;margin-top:14px;padding:10px;" onclick="openStreamerPost('tournament')">← 내 대회 목록으로</button>`);
}

async function spDecide(tourId, pid, approve, title) {
  try {
    await A().decideParticipant(tourId, pid, approve);
    showToast(approve ? '✅ 승인했습니다' : '거절했습니다');
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '처리에 실패했습니다');
  }
  spParticipants(tourId, title); // 목록 새로고침
}

async function spDelete(kind, id) {
  if (!confirm('정말 삭제할까요? 신청 내역도 함께 사라집니다.')) return;
  try {
    await A().deleteContent(kind, id);
    showToast('삭제됐습니다');
    document.getElementById('snuk-dyn-modal').classList.remove('open');
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '삭제에 실패했습니다');
  }
}

async function spSubmit(kind) {
  const title = document.getElementById('spc-title').value.trim();
  if (!title) { showToast('제목을 입력해주세요'); return; }
  const btn = document.getElementById('spc-submit');
  btn.disabled = true;
  btn.textContent = '처리 중...';
  try {
    let img = _spRaw ? (_spRaw.promoImageUrl || _spRaw.bannerImageUrl || null) : null;
    const file = document.getElementById('spc-img').files[0];
    if (file) img = (await A().uploadImage(file)).url;
    const v = (id) => document.getElementById(id).value;
    const desc = v('spc-desc').trim() || null;
    const game = v('spc-game').trim() || null;
    const date = v('spc-date') || null;
    const cap = parseInt(v('spc-cap') || '0', 10) || 0;
    const status = v('spc-status');
    const body = kind === 'tournament'
      ? { title, gameName: game, description: desc, bannerImageUrl: img,
          detailImageUrl: _spRaw ? _spRaw.detailImageUrl : null,
          eventDate: date, applyStart: _spRaw ? _spRaw.applyStart : null, applyEnd: _spRaw ? _spRaw.applyEnd : null,
          capacity: cap, status, resultText: _spRaw ? _spRaw.resultText : null,
          featured: _spRaw ? _spRaw.featured : false, sortOrder: _spRaw ? _spRaw.sortOrder : 0 }
      : { title, gameName: game, description: desc, promoImageUrl: img,
          eventDate: date, applyStart: _spRaw ? _spRaw.applyStart : null, applyEnd: _spRaw ? _spRaw.applyEnd : null,
          status, distributionType: _spRaw ? _spRaw.distributionType : 'APPROVAL',
          keyMode: _spRaw ? _spRaw.keyMode : 'QUANTITY', totalSlots: cap,
          featured: _spRaw ? _spRaw.featured : false, sortOrder: _spRaw ? _spRaw.sortOrder : 0 };
    if (_spEditId) await A().updateContent(kind, _spEditId, body);
    else await A().createContent(kind, body);
    showToast(_spEditId ? '✅ 수정됐습니다' : '✅ 등록됐습니다!');
    document.getElementById('snuk-dyn-modal').classList.remove('open');
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '저장에 실패했습니다');
  } finally {
    btn.disabled = false;
    btn.textContent = _spEditId ? '수정 저장' : '등록하기';
  }
}

// ════════════════════════════════════════════
// 신청 플로우 (캠페인/대회 — 실 API)
// ════════════════════════════════════════════
let pendingApply = null;

function openApply(kind, id) {
  const list = kind === 'tournament' ? (D().mugContents || []) : (D().snukContents || []);
  const item = list.find((x) => x.id === id);
  if (!item) return;
  if (!window.__snukLoggedIn) { showToast('로그인 후 신청할 수 있습니다'); openLogin(); return; }
  pendingApply = { kind, id, title: item.title };
  const sub = document.getElementById('apply-modal-sub');
  if (sub) sub.textContent = `"${item.title}" ${kind === 'tournament' ? '대회 참가' : '컨텐츠'} 신청`;
  document.getElementById('apply-modal').classList.add('open');
}

async function confirmApply() {
  if (!pendingApply) return;
  const { kind, id } = pendingApply;
  const btn = document.getElementById('apply-confirm-btn');
  if (btn) { btn.disabled = true; btn.textContent = '신청 중...'; }
  try {
    if (kind === 'tournament') {
      await A().applyTournament(id);
      showToast('✅ 참가 신청이 접수됐습니다! (운영자 승인 후 확정)');
    } else {
      const res = await A().applyCampaign(id);
      if (res && res.assignedKey) { closeModal('apply-modal'); showAssignedKey(res.assignedKey); }
      else showToast('✅ 신청이 접수됐습니다!');
    }
    closeModal('apply-modal');
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '신청에 실패했습니다');
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = '신청하기'; }
    pendingApply = null;
  }
}

// ════════════════════════════════════════════
// 게임체험단 (콜라보 게임 + 연결 캠페인 + 후기)
// ════════════════════════════════════════════
function initGameTrial() {
  const grid = document.getElementById('game-grid');
  if (!grid) return;
  const games = D().games || [];
  if (!games.length) { grid.innerHTML = emptyCard('모집 중인 게임체험단이 없습니다.'); return; }
  grid.innerHTML = games.map((g, i) => {
    const reviewsHtml = g.reviews.map((r) => `
      <div class="review-item">
        <div class="review-avatar">${esc(r.name.slice(0, 1))}</div>
        <div style="flex:1;min-width:0;">
          <div style="display:flex;align-items:center;gap:6px;margin-bottom:2px;">
            <span class="review-name">${esc(r.name)}</span>
          </div>
          <div class="review-text">${esc(r.text)}</div>
        </div>
      </div>`).join('');
    const full = g.max > 0 && g.members >= g.max;
    const canApply = g.applyOpen && !full && g.campaignId != null;
    return `<div class="game-card" style="width:${cardWidth(3, 1)};min-width:${cardWidth(3, 1)};">
      <div class="game-thumb" style="position:relative;background:${bgOf(i)};">
        ${g.img ? `<img src="${esc(g.img)}" alt="${esc(g.name)}" onerror="this.remove()">` : `<div style="font-size:64px;opacity:.4;">${esc(g.name.slice(0, 1))}</div>`}
        <div class="game-thumb-grad"></div>
        <div style="position:absolute;top:12px;left:12px;z-index:1;">
          <span class="${canApply ? 'badge open' : 'badge closed'}">${canApply ? '모집중' : '마감'}</span>
        </div>
        <div class="game-thumb-caption">
          <div class="game-name">${esc(g.name)}</div>
          ${g.publisher ? `<div class="game-publisher">${esc(g.publisher)}</div>` : ''}
        </div>
      </div>
      <div class="game-body">
        <div class="game-desc">${esc(g.desc)}</div>
        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:0;">
          <span style="font-size:11px;color:var(--text3);">${g.max > 0 ? `신청 ${g.members}/${g.max}` : ''}</span>
          <div style="display:flex;gap:6px;">
            ${g.gameLinkUrl ? `<a href="${esc(g.gameLinkUrl)}" target="_blank" rel="noopener"><button class="btn btn-outline" style="padding:7px 12px;font-size:11px;">게임 링크 ↗</button></a>` : ''}
            <button class="btn-apply" ${canApply ? `onclick="openGame(${i})"` : 'style="background:var(--bg4);color:var(--text3);cursor:default;"'}>${canApply ? '신청하기' : '마감'}</button>
          </div>
        </div>
        <div style="border-top:1px solid var(--border);padding-top:14px;margin-top:auto;">
          <div style="font-size:12px;font-weight:700;color:var(--text2);margin-bottom:10px;">스트리머 후기</div>
          <div>${reviewsHtml || '<div style="font-size:12px;color:var(--text3);">아직 후기가 없습니다.</div>'}</div>
          ${g.campaignId != null ? `<button class="review-more-btn" onclick="window.__snukNav('/campaigns/${g.campaignId}/reviews')" style="width:100%;margin-top:12px;padding:9px;background:transparent;border:1px solid var(--border);border-radius:8px;font-size:12px;font-weight:600;color:var(--text2);cursor:pointer;font-family:'Pretendard','Noto Sans KR',sans-serif;transition:all .2s;" onmouseover="this.style.borderColor='var(--border2)';this.style.color='var(--text)'" onmouseout="this.style.borderColor='var(--border)';this.style.color='var(--text2)'">후기 게시판 (${g.reviewsCount}개)</button>` : ''}
        </div>
      </div>
    </div>`;
  }).join('');
}

// ── 게임 상세 모달 (실 신청 + 실 후기 작성)
let currentGameIdx = null;

function openGame(i) {
  const g = (D().games || [])[i];
  if (!g) return;
  currentGameIdx = i;
  document.getElementById('gm-title').textContent = g.name;
  document.getElementById('gm-pub').textContent = g.publisher || '';
  document.getElementById('gm-desc').textContent = g.desc || '';
  document.getElementById('gm-members').textContent = g.max > 0 ? `${g.members} / ${g.max}` : '—';
  document.getElementById('gm-reviews-count').textContent = `${g.reviewsCount}개`;
  const reviewLink = document.getElementById('gm-review-link');
  if (reviewLink) {
    reviewLink.style.display = g.campaignId != null ? '' : 'none';
    reviewLink.onclick = () => { closeModal('game-modal'); window.__snukNav(`/campaigns/${g.campaignId}/reviews`); };
  }
  document.getElementById('game-modal').classList.add('open');
}

// 발급된 게임 키 모달 (선착순 키 배포 — 신청 즉시 발급)
function showAssignedKey(key) {
  openDynamicModal(`<div class="modal-title">🎁 게임 키가 발급됐습니다!</div>
    <div class="modal-sub">마이페이지 &gt; 게임 코드에서 다시 확인할 수 있어요</div>
    <div style="display:flex;gap:8px;align-items:center;background:var(--bg3);border-radius:10px;padding:14px 16px;">
      <code id="assigned-key-text" style="flex:1;font-size:15px;font-weight:700;color:var(--gold,#ffb300);word-break:break-all;">${esc(key)}</code>
      <button class="btn btn-outline" style="flex-shrink:0;font-size:12px;padding:7px 12px;"
        onclick="navigator.clipboard.writeText(document.getElementById('assigned-key-text').textContent).then(()=>showToast('키가 복사됐습니다'))">복사</button>
    </div>`);
}

async function applyGame() {
  const g = (D().games || [])[currentGameIdx];
  if (!g || g.campaignId == null) return;
  if (!window.__snukLoggedIn) { showToast('로그인 후 신청할 수 있습니다'); openLogin(); return; }
  try {
    const res = await A().applyCampaign(g.campaignId);
    closeModal('game-modal');
    if (res && res.assignedKey) { showAssignedKey(res.assignedKey); }
    else { showToast('✅ 체험단 신청이 접수됐습니다!'); }
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '신청에 실패했습니다');
  }
}

// ── 게임 플레이 영상 슬라이더 (콘텐츠 영상 재사용)
function initGameVideos() {
  const track = document.getElementById('game-videos-slider');
  if (!track) return;
  const vids = D().videos || [];
  const wrap = track.closest('[data-gv-wrap]') || track.parentElement.parentElement;
  if (!vids.length) { wrap.style.display = 'none'; return; }
  wrap.style.display = '';
  const w = cardWidth(6);
  track.innerHTML = vids.map((v, i) => `
    <div class="content-card" style="width:${w};min-width:${w};cursor:pointer;"
      onclick="${v.ytId ? `openGameVideo('${esc(v.ytId)}','${esc(v.title)}')` : `window.open('${esc(v.url)}','_blank')`}">
      <div style="position:relative;aspect-ratio:16/9;background:${bgOf(i)};overflow:hidden;">
        ${v.thumb ? `<img src="${esc(v.thumb)}" alt="" style="width:100%;height:100%;object-fit:cover;display:block;" onerror="this.remove()">` : ''}
        <div style="position:absolute;inset:0;background:rgba(0,0,0,.3);display:flex;align-items:center;justify-content:center;opacity:0;transition:opacity .2s;"
          onmouseover="this.style.opacity='1'" onmouseout="this.style.opacity='0'">
          <div style="width:44px;height:44px;border-radius:50%;background:rgba(255,255,255,.9);display:flex;align-items:center;justify-content:center;font-size:18px;">▶</div>
        </div>
        ${v.ytId ? '<div style="position:absolute;bottom:8px;right:8px;background:#ff0000;border-radius:4px;padding:2px 6px;font-size:9px;font-weight:700;color:#fff;">YT</div>' : ''}
      </div>
      <div style="padding:12px;">
        <div style="font-size:13px;font-weight:700;color:var(--text);line-height:1.4;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(v.title)}</div>
        ${v.meta ? `<div style="font-size:11px;color:var(--text3);margin-top:4px;">${esc(v.meta)}</div>` : ''}
      </div>
    </div>`).join('');
}

function openGameVideo(ytId, title) {
  const modal = document.getElementById('game-video-modal');
  if (!modal) return;
  document.getElementById('gv-iframe').src = `https://www.youtube.com/embed/${ytId}?autoplay=1`;
  document.getElementById('gv-game').textContent = 'SNUK 영상';
  document.getElementById('gv-streamer').textContent = title || '';
  modal.classList.add('open');
}
function closeGameVideo() {
  const modal = document.getElementById('game-video-modal');
  if (modal) { modal.classList.remove('open'); document.getElementById('gv-iframe').src = ''; }
}

// ════════════════════════════════════════════
// 온에어 (2슬롯 플레이어 + 리스트)
// ════════════════════════════════════════════
let videoSlot = 0, activeSlot = [null, null];

function initVideos() {
  const list = document.getElementById('video-list');
  if (!list) return;
  const vids = D().videos || [];
  if (!vids.length) {
    list.innerHTML = `<div style="border:1px dashed var(--border2);border-radius:10px;padding:20px 12px;text-align:center;color:var(--text3);font-size:12px;">등록된 영상이 없습니다.</div>`;
    return;
  }
  list.innerHTML = vids.map((v, i) => `
    <div class="video-item${i === 0 ? ' active' : ''}" onclick="selectVideo(${i},this)">
      <div class="video-mini-thumb">${v.thumb ? `<img src="${esc(v.thumb)}" alt="" onerror="this.remove();">` : ''}</div>
      <div><div class="video-item-title">${esc(v.title)}</div><div class="video-item-meta">${esc(v.meta || '')}</div></div>
    </div>`).join('');
  videoSlot = 0; activeSlot = [null, null];
  selectVideo(0, list.querySelector('.video-item'));
  if (vids.length > 1) { videoSlot = 1; selectVideo(1, list.querySelectorAll('.video-item')[1]); }
  videoSlot = 0;
}

function selectVideo(i) {
  const v = (D().videos || [])[i];
  if (!v) return;
  if (!v.ytId) { window.open(v.url, '_blank'); return; }
  if (activeSlot[1 - videoSlot] === i) videoSlot = 1 - videoSlot;
  activeSlot[videoSlot] = i;
  document.querySelectorAll('.video-item').forEach((e, idx) => {
    e.classList.remove('active', 'active-sub');
    if (idx === activeSlot[0]) e.classList.add('active');
    if (idx === activeSlot[1]) e.classList.add('active-sub');
  });
  const src = `https://www.youtube.com/embed/${v.ytId}?autoplay=0`;
  if (videoSlot === 0) {
    document.getElementById('main-player').innerHTML = `<iframe src="${src}" allowfullscreen></iframe>`;
    document.getElementById('main-title').textContent = v.title;
    document.getElementById('main-meta').textContent = v.meta || '';
  } else {
    document.getElementById('sub-player').innerHTML = `<iframe src="${src}" allowfullscreen></iframe>`;
    document.getElementById('sub-title').textContent = v.title;
    document.getElementById('sub-meta').textContent = v.meta || '';
  }
  videoSlot = 1 - videoSlot;
}

// ════════════════════════════════════════════
// 굿즈 (실데이터 + 준비중 게이트)
// ════════════════════════════════════════════
(function initGoodsModule() {
  function renderGoods() {
    const container = document.getElementById('goods-slider');
    if (!container) return;
    const goods = D().goods || [];
    if (!goods.length) {
      container.innerHTML = emptyCard(D().goodsReady === false ? '굿즈샵 오픈 준비 중입니다. 조금만 기다려주세요!' : '판매 중인 굿즈가 없습니다.');
      window._goodsSlidePages = 1; window._goodsSlideCur = 0;
      return;
    }
    const vw = window.innerWidth;
    let cols, pageSize;
    if (vw <= 768) { cols = 3; pageSize = 6; } else { cols = 5; pageSize = 10; }
    const gap = 10;
    const w = `calc((100% - ${(cols - 1) * gap}px) / ${cols})`;
    const pages = [];
    for (let i = 0; i < goods.length; i += pageSize) pages.push(goods.slice(i, i + pageSize));

    container.innerHTML = pages.map((page) => {
      const row1 = page.slice(0, cols), row2 = page.slice(cols, cols * 2);
      const renderRow = (items) => items.map((g, gi) => {
        const canBuy = g.purchasable;
        const label = g.status === 'ongoing' ? (D().goodsReady === false ? '오픈 준비중' : '판매중') : '품절/종료';
        // 밝은 상품 이미지 위에서도 읽히도록 불투명 다크 칩 고정
        const color = g.status === 'ongoing' ? '#4fdb92' : '#c9c9d2';
        const bg = 'rgba(10,10,14,.78)';
        return `<div class="goods-card" style="width:${w};min-width:${w};">
          <div class="goods-thumb" style="background:${bgOf(gi)};">
            ${g.img ? `<img src="${esc(g.img)}" alt="${esc(g.name)}" onerror="this.remove()">` : ''}
            <div style="position:absolute;top:8px;left:8px;font-size:10px;font-weight:700;padding:3px 8px;border-radius:20px;background:${bg};color:${color};border:1px solid ${color}55;white-space:nowrap;">${label}</div>
          </div>
          <div class="goods-body">
            <div class="goods-name">${esc(g.name)}</div>
            <div class="goods-streamer">${esc(g.streamer)}</div>
            <div class="goods-price">₩${esc(g.price)}</div>
            <div class="goods-actions">
              <button class="goods-btn" style="background:linear-gradient(135deg,var(--accent),var(--accent2));color:#fff;${canBuy ? '' : 'opacity:.4;cursor:not-allowed;'}"
                ${canBuy ? `onclick="buyGoods(${g.id})"` : `onclick="showToast('${D().goodsReady === false ? '굿즈샵 오픈 준비 중입니다' : '구매할 수 없는 상품입니다'}')"`}>구매하기</button>
            </div>
          </div>
        </div>`;
      }).join('');
      return `<div style="display:flex;flex-direction:column;gap:${gap}px;flex-shrink:0;width:100%;min-width:100%;">
        <div style="display:flex;gap:${gap}px;">${renderRow(row1)}</div>
        ${row2.length ? `<div style="display:flex;gap:${gap}px;">${renderRow(row2)}</div>` : ''}
      </div>`;
    }).join('');

    window._goodsSlidePages = pages.length;
    window._goodsSlideCur = 0;
  }
  window._rerenderGoods = renderGoods;
})();

function buyGoods(id) {
  if (!window.__snukLoggedIn) { showToast('로그인 후 구매할 수 있습니다'); openLogin(); return; }
  if (A().buyGoods) A().buyGoods(id);
}

// ════════════════════════════════════════════
// 굿즈 베스트 10 (홈 전용 — 5개×2슬라이드)
// 실 굿즈 0개 = 오픈 준비중 → 홈 섹션 통째로 숨김. 1개라도 등록되면 자동 노출.
// ════════════════════════════════════════════
let goodsBestPage = 0;

function goodsBestTotalPages() { return window._goodsBestPages || 1; }

function initGoodsBest() {
  const track = document.getElementById('goods-best-track');
  if (!track) return;
  const items = (D().goods || []).slice(0, 10);
  const section = document.getElementById('goods-best');
  if (!items.length) {
    // 준비중 게이트: 홈에서 노출 예정이던 섹션만 숨김(비노출 페이지의 none 은 건드리지 않음)
    if (section && section.style.display !== 'none') {
      section.style.display = 'none';
      section.dataset.goodsGate = 'hidden';
    }
    window._goodsBestPages = 1;
    return;
  }
  // 데이터 로드 전 선실행에서 숨겼던 경우 복원
  if (section && section.dataset.goodsGate === 'hidden') {
    section.style.display = '';
    delete section.dataset.goodsGate;
  }
  const perPage = window.innerWidth <= 768 ? 3 : 5;
  const gap = 12;
  const w = `calc((100% - ${(perPage - 1) * gap}px) / ${perPage})`;
  const pages = [];
  for (let i = 0; i < items.length; i += perPage) pages.push(items.slice(i, i + perPage));

  track.innerHTML = pages.map((page, pi) => `<div class="goods-best-page" style="gap:${gap}px;">
    ${page.map((g, gi) => {
      const rank = pi * perPage + gi + 1;
      return `<div class="goods-card" style="width:${w};min-width:${w};" onclick="${g.purchasable ? `buyGoods(${g.id})` : `window.__snukNav('/goods')`}">
        <div class="goods-thumb" style="background:${bgOf(rank)};">
          <div class="goods-rank${rank <= 3 ? ' top3' : ''}">${rank}</div>
          ${g.img ? `<img src="${esc(g.img)}" alt="${esc(g.name)}" onerror="this.remove()">` : ''}
        </div>
        <div class="goods-body">
          <div class="goods-name">${esc(g.name)}</div>
          <div class="goods-streamer">${esc(g.streamer)}</div>
          <div class="goods-price">₩${esc(g.price)}</div>
        </div>
      </div>`;
    }).join('')}
  </div>`).join('');

  window._goodsBestPages = pages.length;
  goodsBestPage = 0;
  track.style.transform = 'translateX(0)';
  renderGoodsBestDots();
  // 굿즈 자동 슬라이드(항목 2) — 4.5초, 호버 시 정지, 끝나면 처음으로
  setAutoLoop('goodsBest', () => {
    const el = document.getElementById('goods-best-track');
    if (!el) return;
    if (hoverPaused(el)) return;
    goGoodsBest((goodsBestPage + 1) % goodsBestTotalPages());
  }, 4500);
}

function renderGoodsBestDots() {
  const dots = document.getElementById('goods-best-dots');
  if (!dots) return;
  dots.innerHTML = Array.from({ length: goodsBestTotalPages() }, (_, i) =>
    `<div onclick="goGoodsBest(${i})" style="width:${i === goodsBestPage ? '18px' : '6px'};height:6px;border-radius:3px;background:${i === goodsBestPage ? 'var(--text)' : 'var(--border2)'};cursor:pointer;transition:all .3s;"></div>`).join('');
}

function slideGoodsBest(dir) { goGoodsBest(goodsBestPage + dir); }
function goGoodsBest(n) {
  const track = document.getElementById('goods-best-track');
  if (!track) return;
  goodsBestPage = Math.max(0, Math.min(goodsBestTotalPages() - 1, n));
  track.style.transform = `translateX(-${goodsBestPage * 100}%)`;
  renderGoodsBestDots();
}

// ════════════════════════════════════════════
// 스트리머 드래그 스트립 (홈 전용 — 푸터 위)
// ════════════════════════════════════════════
function initStreamerStrip() {
  const box = document.getElementById('streamer-strip-scroll');
  if (!box) return;
  const real = D().streamers || [];
  // 실 스트리머 없으면 표시용 더미(운영 요청 — 시안 방식 ddragon 아바타)
  // 실 스트리머 없으면 표시용 더미(이니셜 아바타 — 외부 IP 이미지 사용 금지)
  const items = real.length
    ? real.map((s) => ({ id: s.id, name: s.name, img: s.img, platform: s.platform, followers: s.followers, dummy: false }))
    : Array.from({ length: 12 }, (_, i) => ({
        id: null, name: `스트리머${i + 1}`, platform: 'chz', followers: null, dummy: true, img: null, initial: String(i + 1),
      }));
  box.innerHTML = items.map((s) => {
    const avatar = s.img
      ? `<img src="${esc(s.img)}" alt="${esc(s.name)}" draggable="false" onerror="this.parentElement.textContent='${esc(s.name.slice(0, 1))}';">`
      : `<span style="font-size:24px;font-weight:700;color:${platColor[s.platform] || 'var(--text2)'};">${esc(s.initial || s.name.slice(0, 1))}</span>`;
    const liveRing = s.live ? 'border:2px solid #ff4040;box-shadow:0 0 10px rgba(255,64,64,.5);' : `border:2px solid ${platColor[s.platform] || '#555'}55;`;
    return `<div class="strip-card" onclick="${s.dummy ? `showToast('파트너 스트리머를 기다리고 있어요!')` : `window.__snukNav('/streamers/${s.id}')`}" style="position:relative;">
      ${s.live ? '<span class="strip-live-chip">LIVE</span>' : ''}
      <div class="strip-avatar" style="${liveRing}">${avatar}</div>
      <div class="strip-name">${esc(s.name)}</div>
      <div class="strip-sub" style="color:${platColor[s.platform] || 'var(--text3)'};">${s.live ? '방송 중' : s.followers != null ? `팔로워 ${Number(s.followers).toLocaleString('ko-KR')}` : (platLabel[s.platform] || '')}</div>
    </div>`;
  }).join('');
  bindStripDrag(box);
  // 자동 스크롤(항목 6) — 호버/드래그 중엔 정지, 끝에 닿으면 처음으로
  setAutoLoop('streamerStrip', () => {
    const el = document.getElementById('streamer-strip-scroll');
    if (!el) return;
    if (el.classList.contains('dragging') || el.matches(':hover')) return;
    if (el.scrollLeft + el.clientWidth >= el.scrollWidth - 2) el.scrollLeft = 0;
    else el.scrollLeft += 1;
  }, 30);
}

function bindStripDrag(box) {
  if (box.dataset.dragBound) return;
  box.dataset.dragBound = '1';
  let down = false, moved = false, startX = 0, scrollStart = 0;
  box.addEventListener('pointerdown', (e) => {
    if (e.pointerType !== 'mouse') return; // 터치는 네이티브 스크롤 사용
    down = true; moved = false; startX = e.clientX; scrollStart = box.scrollLeft;
    box.classList.add('dragging');
  });
  box.addEventListener('pointermove', (e) => {
    if (!down) return;
    const dx = e.clientX - startX;
    if (Math.abs(dx) > 6) moved = true;
    box.scrollLeft = scrollStart - dx;
  });
  const up = () => { down = false; box.classList.remove('dragging'); };
  box.addEventListener('pointerup', up);
  box.addEventListener('pointerleave', up);
  box.addEventListener('pointercancel', up);
  // 드래그였다면 카드 클릭 무시
  box.addEventListener('click', (e) => {
    if (moved) { e.stopPropagation(); e.preventDefault(); moved = false; }
  }, true);
}

// ════════════════════════════════════════════
// 협력사 (실 클라이언트 로고 — 카드 그리드, 흑백→호버 컬러)
// ════════════════════════════════════════════
function initPartners() {
  const grid = document.getElementById('partner-grid');
  if (!grid) return;
  const partners = D().partners || [];
  if (!partners.length) {
    grid.innerHTML = `<div style="grid-column:1/-1;">${emptyCard('등록된 협력사가 없습니다.')}</div>`;
    return;
  }
  grid.innerHTML = partners.map((p) => {
    const logo = `<div class="partner-card-logo">
      <img src="${esc(p.logoUrl)}" alt="${esc(p.name)}" onerror="this.replaceWith(document.createTextNode('${esc(p.name)}'))">
    </div>`;
    const inner = `${logo}<div class="partner-card-name">${esc(p.name)}</div>`;
    return p.linkUrl
      ? `<a class="partner-card" href="${esc(p.linkUrl)}" target="_blank" rel="noopener">${inner}</a>`
      : `<div class="partner-card">${inner}</div>`;
  }).join('');
}

// ════════════════════════════════════════════
// 챔피언십: 대진표(결과 기반) + 참여 스트리머 로스터(실 참가자)
// ════════════════════════════════════════════
function bracketEmptyCard() {
  const mug = D().mugFeatured;
  if (mug && mug.resultText) {
    return `<div style="width:100%;background:var(--bg2);border:1px solid var(--border);border-radius:14px;padding:24px;">
      <div style="font-size:11px;font-weight:700;letter-spacing:2px;color:#ffb300;margin-bottom:12px;">RESULT — ${esc(mug.title)}</div>
      <div style="font-size:14px;color:var(--text);line-height:1.9;white-space:pre-wrap;">${esc(mug.resultText)}</div>
    </div>`;
  }
  return `<div style="width:100%;">${emptyCard('경기 일정이 확정되면 대진표가 공개됩니다.')}</div>`;
}

function initBracket() {
  // 경기(대진) 데이터 모델이 아직 없음 — 강수 탭(16강 등) UI 는 대진 데이터가 생길 때까지 통째로 숨김.
  // (박격포대회처럼 토너먼트 형식이 아닌 대회에선 "몇강"이 무의미)
  const pcWrap = document.getElementById('desktop-bracket-wrap');
  if (pcWrap) pcWrap.style.display = 'none';
  const mbCont = document.getElementById('mb-match-container');
  const mbWrap = mbCont && mbCont.parentElement ? mbCont.parentElement.parentElement : null;
  if (mbWrap) mbWrap.style.display = 'none';
}
function switchPcTab(_k, btn) { document.querySelectorAll('.pc-bt').forEach((t) => t.classList.remove('active')); btn.classList.add('active'); }
function switchMbTab(_k, btn) { document.querySelectorAll('.mb-tab').forEach((t) => t.classList.remove('active')); btn.classList.add('active'); }
function slidePcMatch() {}
function slideMbMatch() {}
function goPcMatch() {}
function goMbMatch() {}

// ── 참여 스트리머 로스터
const ROSTER_PER_PAGE = 8;
let rosterPage = 0;
const platColor = { chz: '#00c73c', soop: '#34c7ff', yt: '#ff4040', cime: '#7c5cff' };
const platShort = { chz: '치', soop: '숲', yt: 'YT', cime: '씨' };
const platLabel = { chz: '치지직', soop: '숲', yt: '유튜브', cime: '씨미' };

function rosterTotalPages() { return Math.max(1, Math.ceil((D().roster || []).length / ROSTER_PER_PAGE)); }

function initRoster() {
  const track = document.getElementById('roster-track');
  const dots = document.getElementById('roster-dots');
  if (!track) return;
  const roster = D().roster || [];

  const headerLabel = document.getElementById('roster-count-label');
  if (headerLabel) headerLabel.textContent = `${D().rosterTournamentTitle || ''} · ${roster.length}명`;

  if (!roster.length) {
    track.style.flexWrap = 'nowrap';
    track.innerHTML = `<div style="width:100%;">${emptyCard('승인된 참가자가 아직 없습니다.')}</div>`;
    if (dots) dots.innerHTML = '';
    const lbl = document.getElementById('roster-page-label');
    if (lbl) lbl.textContent = '';
    return;
  }

  // 전원 노출 그리드(페이징 없음) — 프사·이름 가독성 우선
  const navWrap = track.parentElement ? track.parentElement.parentElement : null;
  if (navWrap) navWrap.querySelectorAll('button[onclick^="slideRoster"]').forEach((b) => { b.style.display = 'none'; });
  track.style.flexWrap = 'wrap';
  track.style.transform = 'none';
  track.style.justifyContent = 'flex-start';
  track.innerHTML = roster.map((s) => {
    const avatar = s.img
      ? `<img src="${esc(s.img)}" alt="${esc(s.name)}" onerror="this.parentElement.style.background='var(--bg3)';this.remove()">`
      : `<span style="font-size:16px;font-weight:700;color:${platColor[s.platform] || 'var(--text2)'};width:100%;height:100%;display:flex;align-items:center;justify-content:center;">${esc(s.name.slice(0, 1))}</span>`;
    const inner = `
      <div class="roster-card" style="border-color:var(--border);position:relative;">
        <div style="position:absolute;top:-6px;left:50%;transform:translateX(-50%);background:rgba(52,199,120,.14);border:1px solid rgba(52,199,120,.4);border-radius:20px;padding:1px 6px;font-size:8px;font-weight:700;color:#34c878;white-space:nowrap;z-index:2;">참가확정</div>
        <div class="roster-avatar" style="width:64px;height:64px;border:2px solid ${platColor[s.platform] || '#555'}55;margin-top:10px;">
          ${avatar}
          <div style="position:absolute;bottom:-2px;right:-2px;width:16px;height:16px;border-radius:50%;background:${platColor[s.platform] || '#555'};display:flex;align-items:center;justify-content:center;font-size:8px;font-weight:700;color:#fff;border:2px solid var(--bg2);">${platShort[s.platform] || '?'}</div>
        </div>
        <div style="font-size:12px;font-weight:700;color:var(--text);text-align:center;width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;padding:0 4px;margin-top:6px;">${esc(s.name)}</div>
        <div style="font-size:10px;color:${platColor[s.platform] || 'var(--text3)'};font-weight:600;text-align:center;margin-bottom:6px;">${platLabel[s.platform] || ''}</div>
      </div>`;
    return s.streamUrl
      ? `<a href="${esc(s.streamUrl)}" target="_blank" rel="noopener" style="text-decoration:none;flex-shrink:0;width:112px;min-width:112px;">${inner}</a>`
      : `<div style="flex-shrink:0;width:112px;min-width:112px;">${inner}</div>`;
  }).join('');

  if (dots) dots.innerHTML = '';
  const pageLbl = document.getElementById('roster-page-label');
  if (pageLbl) pageLbl.textContent = '';
}

function renderRosterPage() {
  const track = document.getElementById('roster-track');
  if (!track || !track.children[0]) return;
  const totalCards = (D().roster || []).length;
  if (!totalCards) return;
  const cardW = track.scrollWidth / totalCards;
  const moveW = cardW > 0 ? cardW : (track.parentElement?.offsetWidth || 600) / 8;
  track.style.transform = `translateX(-${rosterPage * ROSTER_PER_PAGE * moveW}px)`;
  for (let i = 0; i < rosterTotalPages(); i++) {
    const d = document.getElementById(`rdot-${i}`);
    if (d) { d.style.width = i === rosterPage ? '18px' : '6px'; d.style.background = i === rosterPage ? 'var(--accent)' : 'var(--border2)'; }
  }
  const lbl = document.getElementById('roster-page-label');
  if (lbl) lbl.textContent = `${rosterPage + 1} / ${rosterTotalPages()}`;
}
function slideRoster(dir) { rosterPage = Math.max(0, Math.min(rosterTotalPages() - 1, rosterPage + dir)); renderRosterPage(); }
function goRosterPage(n) { rosterPage = n; renderRosterPage(); }

// ── 스트리머 채널 섹션 (실 스트리머 등급 회원)
let streamerChanPos = 0;

let _streamerQuery = '';
function filterStreamerChannels(q) {
  _streamerQuery = (q || '').trim().toLowerCase();
  initStreamerChannels();
}

function initStreamerChannels() {
  const track = document.getElementById('streamer-ch-track');
  if (!track) return;
  let streamers = D().streamers || [];
  if (_streamerQuery) {
    streamers = streamers.filter((s) => s.name.toLowerCase().includes(_streamerQuery));
  }
  if (!streamers.length) {
    track.innerHTML = emptyCard(_streamerQuery ? `"${_streamerQuery}" 검색 결과가 없습니다.` : '아직 등록된 파트너 스트리머가 없습니다.');
    return;
  }
  const mw = window.innerWidth;
  let cols;
  if (mw <= 480) cols = 3;
  else if (mw <= 768) cols = 4;
  else cols = 6;
  const gap16 = (cols - 1) * 16;
  const w = `calc((100% - ${gap16}px) / ${cols})`;
  track.innerHTML = streamers.map((s) => {
    const avatar = s.img
      ? `<img src="${esc(s.img)}" alt="${esc(s.name)}" onerror="this.parentElement.style.background='var(--bg3)';this.remove()">`
      : `<span style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:700;color:${platColor[s.platform] || 'var(--text2)'};background:var(--bg3);">${esc(s.name.slice(0, 1))}</span>`;
    const platBtn = s.channelUrl
      ? `<a href="${esc(s.channelUrl)}" target="_blank" rel="noopener" class="streamer-ch-plat"
          style="background:${platColor[s.platform]}18;color:${platColor[s.platform]};border:1px solid ${platColor[s.platform]}44;"
          onclick="event.stopPropagation()">${platLabel[s.platform] || ''}</a>`
      : `<span class="streamer-ch-plat" style="background:${platColor[s.platform]}18;color:${platColor[s.platform]};border:1px solid ${platColor[s.platform]}44;">${platLabel[s.platform] || ''}</span>`;
    return `<div class="streamer-ch-item" style="width:${w};min-width:${w};cursor:pointer;position:relative;" onclick="window.__snukNav('/streamers/${s.id}')">
      ${s.live ? '<span class="strip-live-chip" style="position:absolute;top:4px;right:10px;">LIVE</span>' : ''}
      <div class="streamer-ch-avatar" style="${s.live ? 'box-shadow:0 0 0 2px #ff4040, 0 0 12px rgba(255,64,64,.45);border-radius:50%;' : ''}">${avatar}</div>
      <div class="streamer-ch-name">${esc(s.name)}</div>
      ${s.live && s.liveTitle ? `<div style="font-size:10px;color:#ff8080;text-align:center;margin-bottom:4px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;padding:0 6px;">🔴 ${esc(s.liveTitle)}</div>`
        : s.followers != null ? `<div style="font-size:10px;color:var(--text3);text-align:center;margin-bottom:4px;">팔로워 ${Number(s.followers).toLocaleString('ko-KR')}</div>` : ''}
      <div class="streamer-ch-plats">${platBtn}</div>
    </div>`;
  }).join('');
}

function slideStreamers(dir) {
  const track = document.getElementById('streamer-ch-track');
  if (!track || !track.children[0]) return;
  const gap = 16;
  const cardW = track.children[0].offsetWidth + gap;
  const clipW = track.parentElement.offsetWidth;
  const visible = Math.max(1, Math.floor(clipW / cardW));
  const max = Math.max(0, track.children.length - visible);
  streamerChanPos = Math.max(0, Math.min(max, streamerChanPos + dir));
  track.style.transform = `translateX(-${streamerChanPos * cardW}px)`;
}

// ════════════════════════════════════════════
// SLIDER ENGINE
// ════════════════════════════════════════════
const sliderPos = {};
function slide(id, dir) {
  const realId = id === 'game-slider' ? 'game-grid' : id;
  const track = document.getElementById(realId);
  if (!track) return;

  if (id === 'goods-slider') {
    const pages = window._goodsSlidePages || 1;
    window._goodsSlideCur = Math.max(0, Math.min(pages - 1, (window._goodsSlideCur || 0) + dir));
    track.style.transform = `translateX(-${window._goodsSlideCur * 100}%)`;
    return;
  }

  const cards = track.querySelectorAll('.content-card,.goods-card,.game-card');
  if (!cards.length) return;
  const gap = 16;
  const cardW = cards[0].offsetWidth + gap;
  const clipEl = track.parentElement;
  const clipW = clipEl.offsetWidth;
  const visible = Math.max(1, Math.round(clipW / cardW));
  const max = Math.max(0, cards.length - visible);
  sliderPos[id] = sliderPos[id] || 0;
  sliderPos[id] = Math.max(0, Math.min(max, sliderPos[id] + dir));
  track.style.transform = `translateX(-${sliderPos[id] * cardW}px)`;
}

// ════════════════════════════════════════════
// CHZZK LIVE (공식 채널 — config 주입)
// ════════════════════════════════════════════
let chatSimInterval = null;

function chzzkId() { return D().chzzkChannelId || ''; }

function openChzzkSite() {
  const id = chzzkId();
  window.open(id ? `https://chzzk.naver.com/live/${id}` : 'https://chzzk.naver.com', '_blank');
}

function loadChzzk() {
  const id = chzzkId();
  if (!id) { showToast('공식 채널 라이브는 준비 중입니다'); return; }
  const ph = document.getElementById('chzzk-placeholder'), ct = document.getElementById('chzzk-iframe-container');
  const ov = document.getElementById('chzzk-overlay'), lb = document.getElementById('chzzk-channel-label');
  if (!ph || !ct) return;
  ct.innerHTML = `<iframe src="https://chzzk.naver.com/live/${id}" style="width:100%;aspect-ratio:16/9;border:none;display:block;" allowfullscreen allow="autoplay;fullscreen"></iframe>`;
  ph.style.display = 'none'; ct.style.display = 'block'; if (ov) ov.style.display = 'block';
  if (lb) lb.textContent = 'SNUK 공식채널';
  const cb = document.getElementById('chat-channel-badge'); if (cb) { cb.textContent = 'SNUK'; cb.style.display = 'inline-block'; }
  const cc = document.getElementById('chzzk-chat-container');
  if (cc) cc.innerHTML = `<iframe src="https://chzzk.naver.com/chat/${id}" style="width:100%;height:100%;min-height:240px;border:none;display:block;"></iframe>`;
  toggleChatEmbed();
  showToast('SNUK 공식 채널 라이브 로드 중...');
}

function toggleChatEmbed() {
  const ev = document.getElementById('chat-embed-view'), sv = document.getElementById('chat-sim-view');
  if (!ev || !sv) return;
  ev.style.display = 'flex'; sv.style.display = 'none';
  const tb = document.getElementById('chat-toggle-btn'), mb = document.getElementById('chat-mode-btn');
  if (tb) tb.style.cssText = 'background:var(--accent);color:#fff;border-color:var(--accent);';
  if (mb) mb.style.cssText = 'background:var(--bg3);color:var(--text2);border-color:var(--border);';
  stopChatSim();
}
function toggleChatMode() {
  const ev = document.getElementById('chat-embed-view'), sv = document.getElementById('chat-sim-view');
  if (!ev || !sv) return;
  ev.style.display = 'none'; sv.style.display = 'flex';
  const tb = document.getElementById('chat-toggle-btn'), mb = document.getElementById('chat-mode-btn');
  if (mb) mb.style.cssText = 'background:var(--accent);color:#fff;border-color:var(--accent);';
  if (tb) tb.style.cssText = 'background:var(--bg3);color:var(--text2);border-color:var(--border);';
}
function startChatSim() {}
function stopChatSim() { if (chatSimInterval) { clearInterval(chatSimInterval); chatSimInterval = null; } }

function addChatMessage(u, text) {
  const box = document.getElementById('chat-messages');
  if (!box) return;
  const wrap = document.createElement('div');
  wrap.className = 'chat-msg';
  wrap.innerHTML = `<div class="chat-avatar" style="background:${u.color}22;color:${u.color};">${esc(u.name.slice(0, 1))}</div><div style="flex:1;min-width:0;"><span class="chat-name" style="color:${u.color};">${esc(u.name)}</span><span class="chat-text">${esc(text)}</span></div>`;
  box.appendChild(wrap);
  while (box.children.length > 60) box.removeChild(box.firstChild);
  box.scrollTop = box.scrollHeight;
}
function myChatName() {
  return (window.__snukUser && window.__snukUser.nickname) || '나';
}
function sendChat() {
  const i = document.getElementById('chat-input');
  const t = (i.value || '').trim();
  if (!t) return;
  if (!window.__snukLoggedIn) { showToast('로그인 후 채팅에 참여할 수 있습니다'); openLogin(); return; }
  addChatMessage({ name: myChatName(), color: 'var(--accent)' }, t);
  i.value = '';
}
function quickChat(t) {
  if (!window.__snukLoggedIn) { showToast('로그인 후 채팅에 참여할 수 있습니다'); openLogin(); return; }
  addChatMessage({ name: myChatName(), color: 'var(--accent)' }, t);
}

// ════════════════════════════════════════════
// FILTERS
// ════════════════════════════════════════════
function filterMug(s, btn) {
  document.querySelectorAll('#mug-tabs .tab').forEach((t) => t.classList.remove('active')); btn.classList.add('active');
  const all = D().mugContents || [];
  const f = s === 'all' ? all : all.filter((d) => d.status === s);
  initSlider('mug-slider', f.length ? f.map((d, i) => makeContentCard(d, cardWidth(6), i)).join('') : emptyCard('해당 상태의 대회가 없습니다.'));
}
function filterEvt(s, btn) {
  document.querySelectorAll('#evt-tabs .tab').forEach((t) => t.classList.remove('active')); btn.classList.add('active');
}
function filterStr(s, btn) {
  document.querySelectorAll('#str-tabs .tab').forEach((t) => t.classList.remove('active')); btn.classList.add('active');
}
function filterJobs(s, btn) {
  document.querySelectorAll('#job-tabs .tab').forEach((t) => t.classList.remove('active')); btn.classList.add('active');
}

// ════════════════════════════════════════════
// AUTH (실 OAuth — SnukShell 이 __snukLoggedIn/__snukUser 관리)
// ════════════════════════════════════════════
function openLogin() { document.getElementById('login-modal').classList.add('open'); }
function openSignup() { document.getElementById('signup-modal').classList.add('open'); }
function doLogin(p) {
  const map = { '치지직': 'chzzk', '씨미': 'cime', '숲': 'soop' };
  location.href = '/oauth2/authorization/' + (map[p] || 'chzzk');
}
function doLogout() { if (window.__snukLogout) window.__snukLogout(); }
function openMypage() {
  if (window.__snukLoggedIn) { window.__snukNav ? window.__snukNav('/mypage') : (location.href = '/mypage'); }
  else openLogin();
}
function mpTab(panel, btn) {
  document.querySelectorAll('.mypage-tab').forEach((t) => t.classList.remove('active')); btn.classList.add('active');
  document.querySelectorAll('.mypage-panel').forEach((p) => p.classList.remove('active'));
  const el = document.getElementById('mp-' + panel);
  if (el) el.classList.add('active');
}

// ════════════════════════════════════════════
// 공지사항 (실데이터)
// ════════════════════════════════════════════
function renderNotices() {
  const list = document.getElementById('rs-notice-list');
  if (!list) return;
  const notices = D().notices || [];
  if (!notices.length) {
    list.innerHTML = '<div style="font-size:12px;color:var(--text3);padding:6px 0;">등록된 공지가 없습니다.</div>';
    return;
  }
  list.innerHTML = notices.slice(0, 3).map((n) => `
    <div class="rs-ni" onclick="showNotice(${n.id})">
      <div class="rs-ni-t">${n.pinned ? '📌 ' : ''}${esc(n.title)}</div>
      <div class="rs-ni-d">${esc(n.date)}</div>
    </div>`).join('');
}

function showNotice(id) {
  const n = (D().notices || []).find((x) => x.id === id);
  if (!n) return;
  openDynamicModal(`<div class="modal-title">${esc(n.title)}</div>
    <div class="modal-sub">${esc(n.date)}</div>
    <div style="background:var(--bg3);border-radius:10px;padding:16px;font-size:13px;color:var(--text);line-height:1.8;white-space:pre-wrap;">${esc(n.content || '내용이 없습니다.')}</div>`);
}

function showNoticeHome() {
  const notices = D().notices || [];
  openDynamicModal(`<div class="modal-title">공지사항</div>
    <div class="modal-sub">SNUK 소식을 확인하세요</div>
    ${notices.length ? notices.map((n) => `
      <div style="border-bottom:1px solid var(--border);padding:12px 2px;cursor:pointer;" onclick="showNotice(${n.id})">
        <div style="font-size:13px;font-weight:600;color:var(--text);">${n.pinned ? '📌 ' : ''}${esc(n.title)}</div>
        <div style="font-size:11px;color:var(--text3);margin-top:3px;">${esc(n.date)}</div>
      </div>`).join('') : '<div style="font-size:13px;color:var(--text3);">등록된 공지가 없습니다.</div>'}`);
}

// ════════════════════════════════════════════
// 스포트라이트 (실데이터 + 실 등록)
// ════════════════════════════════════════════
let selectedPlatform = 'chz';
function selectPlatform(btn, plat) {
  selectedPlatform = plat;
  const colors = { chz: '#00c73c', soop: '#34c7ff', yt: '#ff4040' };
  document.querySelectorAll('.sp-plat-btn').forEach((b) => {
    b.style.borderColor = 'var(--border)';
    b.style.background = 'transparent';
    b.style.color = 'var(--text2)';
  });
  btn.style.borderColor = colors[plat];
  btn.style.background = `${colors[plat]}18`;
  btn.style.color = colors[plat];
}

async function submitSpotlight() {
  const title = (document.getElementById('sp-title') || {}).value || '';
  const url = (document.getElementById('sp-url') || {}).value || '';
  if (!title.trim()) { showToast('방송 제목을 입력해주세요'); return; }
  if (!/^https:\/\//.test(url.trim())) { showToast('https:// 로 시작하는 방송 링크를 입력해주세요'); return; }
  const platformMap = { chz: 'CHZZK', soop: 'SOOP', yt: 'YOUTUBE' };
  try {
    await A().createSpotlight({ title: title.trim(), platform: platformMap[selectedPlatform] || 'CHZZK', streamUrl: url.trim() });
    showToast('✅ 스포트라이트가 등록됐습니다! (2시간 노출)');
    closeModal('spotlight-modal');
    if (A().reloadData) A().reloadData();
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '등록에 실패했습니다 (스트리머만 등록 가능)');
  }
}

function initSpotlight() {
  const cards = document.getElementById('spotlight-cards');
  if (!cards) return;
  const spotlights = D().spotlights || [];
  const section = document.getElementById('spotlight-section');

  if (!spotlights.length) {
    // 활성 스포트라이트 없으면 홈 섹션 통째로 숨김(비노출 페이지의 none 은 유지)
    if (section && section.style.display !== 'none') {
      section.style.display = 'none';
      section.dataset.spotGate = 'hidden';
    }
    return;
  }
  // 데이터 로드 전 선실행에서 숨겼던 경우 복원
  if (section && section.dataset.spotGate === 'hidden') {
    section.style.display = '';
    delete section.dataset.spotGate;
  }

  const pc = { chz: '#00c73c', soop: '#34c7ff', yt: '#ff4040' };
  const pn = { chz: '치지직', soop: '숲', yt: '유튜브' };

  cards.innerHTML = '<div class="spotlight-grid">' + spotlights.slice(0, 4).map((s) => `
    <div class="spotlight-card" onclick="window.open('${esc(s.url)}','_blank')">
      <div class="spotlight-screen">
        ${s.img ? `<img src="${esc(s.img)}" alt="${esc(s.name)}" onerror="this.remove()">` : ''}
        <div class="spotlight-screen-overlay"></div>
        <div class="spotlight-live">LIVE</div>
      </div>
      <div class="spotlight-bottom">
        <div class="spotlight-meta">
          <div class="spotlight-name">${esc(s.name)}</div>
          <div class="spotlight-plat" style="color:${pc[s.platform] || '#aaa'};">${pn[s.platform] || ''}</div>
        </div>
        <div class="spotlight-sub">${esc(s.sub)}</div>
      </div>
    </div>`).join('') + '</div>';
}

function openSpotlight() {
  if (!window.__snukLoggedIn) { showToast('로그인 후 등록할 수 있습니다'); openLogin(); return; }
  document.getElementById('spotlight-modal').classList.add('open');
}

// ════════════════════════════════════════════
// 검색 (실데이터 통합 검색)
// ════════════════════════════════════════════
function snukSearch(q) {
  const query = (q || '').trim().toLowerCase();
  if (!query) return;
  const d = D();
  const results = [];
  (d.snukContents || []).forEach((x) => { if (x.title.toLowerCase().includes(query)) results.push({ label: x.title, cat: 'SNUK 컨텐츠', path: '/campaigns' }); });
  (d.mugContents || []).forEach((x) => { if (x.title.toLowerCase().includes(query)) results.push({ label: x.title, cat: '대회', path: '/championship' }); });
  (d.games || []).forEach((x) => { if (x.name.toLowerCase().includes(query)) results.push({ label: x.name, cat: '게임체험단', path: '/campaigns' }); });
  (d.goods || []).forEach((x) => { if (x.name.toLowerCase().includes(query)) results.push({ label: x.name, cat: '굿즈', path: '/goods' }); });
  (d.videos || []).forEach((x) => { if (x.title.toLowerCase().includes(query)) results.push({ label: x.title, cat: '영상', path: '/videos' }); });

  openDynamicModal(`<div class="modal-title">검색: ${esc(q)}</div>
    <div class="modal-sub">${results.length}건의 결과</div>
    ${results.length ? results.slice(0, 20).map((r) => `
      <div style="border-bottom:1px solid var(--border);padding:12px 2px;cursor:pointer;display:flex;align-items:center;gap:8px;"
        onclick="document.getElementById('snuk-dyn-modal').classList.remove('open');window.__snukNav('${r.path}')">
        <span style="font-size:10px;font-weight:700;color:var(--accent);background:var(--bg3);border-radius:6px;padding:2px 8px;flex-shrink:0;">${esc(r.cat)}</span>
        <span style="font-size:13px;color:var(--text);">${esc(r.label)}</span>
      </div>`).join('') : '<div style="font-size:13px;color:var(--text3);">검색 결과가 없습니다.</div>'}`);
}
window.__snukSearch = snukSearch;

// ════════════════════════════════════════════
// MODALS / TOAST / THEME / 모바일 메뉴
// ════════════════════════════════════════════
function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('open');
  if (id === 'game-video-modal') { const f = document.getElementById('gv-iframe'); if (f) f.src = ''; }
}
function bindOverlayClose() {
  document.querySelectorAll('.modal-overlay').forEach((m) => {
    if (m.dataset.bound) return;
    m.dataset.bound = '1';
    m.addEventListener('click', function (e) { if (e.target === this) this.classList.remove('open'); });
  });
}
function setStars() {}

let toastTimer;
function showToast(msg) {
  const t = document.getElementById('toast');
  if (!t) return;
  document.getElementById('toast-msg').textContent = msg;
  t.classList.add('show'); clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove('show'), 2500);
}

function toggleMobileMenu() {
  const drawer = document.getElementById('mobile-drawer');
  const overlay = document.getElementById('mobile-overlay');
  const btn = document.getElementById('mobile-menu-btn');
  const isOpen = drawer.classList.contains('open');
  if (isOpen) { closeMobileMenu(); } else {
    drawer.classList.add('open'); overlay.classList.add('open'); btn.classList.add('open');
  }
}
function closeMobileMenu() {
  document.getElementById('mobile-drawer').classList.remove('open');
  document.getElementById('mobile-overlay').classList.remove('open');
  document.getElementById('mobile-menu-btn').classList.remove('open');
}

// 현재 라우트에 맞춰 사이드바 active 표시 (SnukShell 이 라우트 변경 시 호출)
function setActiveNav(path) {
  document.querySelectorAll('.rs-item').forEach((btn) => {
    const oc = btn.getAttribute('onclick') || '';
    const m = oc.match(/__snukNav\('([^']+)'\)/);
    btn.classList.toggle('active', !!m && (m[1] === path || (m[1] !== '/' && path.startsWith(m[1]))));
  });
}
window.__snukSetActiveNav = setActiveNav;

// 리사이즈 시 재렌더
let _resizeTimer;
window.addEventListener('resize', function () {
  clearTimeout(_resizeTimer);
  _resizeTimer = setTimeout(function () {
    renderContentSliders();
    initBigContents();
    initNews();
    initGameTrial();
    initGameVideos();
    if (window._rerenderGoods) window._rerenderGoods();
    initGoodsBest();
    initPartners();
    initRoster();
    streamerChanPos = 0;
    initStreamerChannels();
    Object.keys(sliderPos).forEach((k) => { sliderPos[k] = 0; const t = document.getElementById(k === 'game-slider' ? 'game-grid' : k); if (t) t.style.transform = 'translateX(0)'; });
  }, 200);
});

// ════════════════════════════════════════════
// INIT (페이지 마운트마다 SnukSections/SnukShell 이 호출 — 멱등)
// ════════════════════════════════════════════
function __snukInit() {
  const chatInp = document.getElementById('chat-input');
  if (chatInp && !chatInp.dataset.bound) {
    chatInp.dataset.bound = '1';
    chatInp.addEventListener('keydown', (e) => { if (e.key === 'Enter') sendChat(); });
  }
  bindOverlayClose();
  renderContentSliders();
  initBigContents();
  initLiveBanner();
  initNews();
  renderFeatured('snuk-featured', D().snukFeatured, 'FEATURED');
  renderFeatured('mug-featured', D().mugFeatured, 'SIGNATURE CONTENT');
  initGameTrial();
  initGameVideos();
  initVideos();
  if (window._rerenderGoods) window._rerenderGoods();
  initGoodsBest();
  initStreamerStrip();
  initPartners();
  initBracket();
  initRoster();
  initStreamerChannels();
  initSpotlight();
  initStreamerPost();
  renderNotices();
  applySiteImages();
  initHeroStats();
  setActiveNav(location.pathname);
}

// 히어로 실데이터 스탯 (파트너 스트리머 / 모집중 컨텐츠 / 대회)
function initHeroStats() {
  const el = document.getElementById('hero-stats');
  if (!el) return;
  const d = D() || {};
  const streamers = (d.streamers || []).length;
  const openContents = (d.snukContents || []).filter((c) => c.status === 'open').length;
  const activeTours = (d.mugContents || []).filter((t) => t.status === 'open' || t.status === 'ongoing').length;
  const stat = (n, label) => `<div class="hero-stat"><strong>${n}</strong><span>${label}</span></div>`;
  el.innerHTML = stat(streamers, '파트너 스트리머') + stat(openContents, '모집중 컨텐츠') + stat(activeTours, '진행 · 예정 대회');
}

// 어드민 "설정" 탭에서 바꾼 히어로/배너 이미지·문구 적용 ('-'=미설정 → 마크업 기본값 유지)
function applySiteImages() {
  const ss = (D() && D().siteSettings) || {};
  const set = (v) => v && v !== '-';
  const applyImg = (sel, url) => {
    if (!set(url)) return;
    const el = document.querySelector(sel);
    if (el && el.getAttribute('src') !== url) el.setAttribute('src', url);
  };
  const applyText = (sel, text) => {
    const el = document.querySelector(sel);
    if (!el) return;
    if (text === undefined || text === null || text === '-') return; // 미설정 → 기본 문구 유지
    if (String(text).trim() === '') { el.style.display = 'none'; return; } // 빈값 저장 → 문구 숨김(항목 4)
    el.style.display = '';
    el.textContent = text;
  };
  applyImg('#hero .hero-banner-card > img', ss.HERO_IMAGE_URL);
  // 페이지 배너: 이미지 + 제목 + 문구 (키=BANNER_{PAGE}_{URL|TITLE|SUB}, V12 시드)
  const BANNER_SECTIONS = {
    CONTENTS: '#snuk-contents .page-banner',
    CHAMPIONSHIP: '#mugchamps .page-banner',
    GAMES: '#game-trial .page-banner',
    VIDEOS: '#videos .page-banner',
    STREAMERS: '#streamers-channel .page-banner',
    GOODS: '#goods .goods-banner',
    PARTNERS: '#partners .goods-banner',
  };
  for (const [page, sel] of Object.entries(BANNER_SECTIONS)) {
    applyImg(`${sel} img`, ss[`BANNER_${page}_URL`]);
    applyText(`${sel} .goods-banner-text h2`, ss[`BANNER_${page}_TITLE`]);
    applyText(`${sel} .goods-banner-text p`, ss[`BANNER_${page}_SUB`]);
  }
}

;(function () { try { if (typeof __snukInit === 'function') __snukInit(); } catch (e) { console.error('[snuk init]', e); } })();
