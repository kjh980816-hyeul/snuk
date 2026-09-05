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
// 데모 시안 .contcard — 썸네일+D-day / 종류·상태 태그 / 소개 / 스펙 / 주최 byline / 와이드 버튼
function ddayOf(eventDate) {
  if (!eventDate) return null;
  const d = Math.ceil((new Date(`${eventDate}T00:00:00`).getTime() - Date.now()) / 86400000);
  return Number.isFinite(d) ? d : null;
}
// ── 상태 공용 헬퍼 (서버 판정값 applyOpen/preparing 기준. status 문자열은 표시 분기용)
function isPreparing(d) { return !!(d && (d.preparing || d.status === 'preparing')); }
function canApplyOf(d) { return !!(d && (d.applyOpen != null ? d.applyOpen : d.status === 'open')); }
function badgeClsOf(d) {
  return isPreparing(d) ? 'preparing' : d.status === 'open' ? 'open' : d.status === 'ongoing' ? 'ongoing' : 'closed';
}
function tagClsOf(d) {
  return isPreparing(d) ? 't-prep' : d.status === 'open' ? 't-go' : d.status === 'ongoing' ? 't-warn' : 't-neu';
}
// 카드 클릭 액션: 대회=상세 페이지 / 모집중=신청 모달 / 준비중=내용만 보기 모달 / 그 외=컨텐츠 목록
function cardClickOf(d) {
  if (d.kind === 'tournament') return `__snukNav('/championship/${d.id}')`;
  if (canApplyOf(d)) return `openApply('${d.kind}',${d.id})`;
  if (isPreparing(d)) return `openPreview('${d.kind}',${d.id})`;
  return `window.__snukNav('/campaigns')`;
}
// 준비중 컨텐츠 — 내용(이미지·제목·소개)만 보여주는 읽기 전용 모달. 모집 정보·신청 버튼 없음.
function openPreview(kind, id) {
  const list = kind === 'tournament' ? (D().mugContents || []) : (D().snukContents || []);
  let item = list.find((x) => x.id === id);
  if (!item && kind === 'campaign') {
    const g = (D().games || []).find((x) => x.campaignId === id);
    if (g) item = { id, title: g.name, desc: g.desc, img: g.img, statusLabel: '준비중', preparing: true };
  }
  if (!item) return;
  openDynamicModal(`
    <div class="modal-title">${esc(item.title)}</div>
    <div class="modal-sub"><span class="badge preparing">${esc(item.statusLabel || '준비중')}</span>
      <span style="margin-left:8px;">모집이 시작되면 신청할 수 있어요</span></div>
    ${item.img ? `<div style="border-radius:12px;overflow:hidden;margin:12px 0;background:var(--bg3);"><img src="${esc(item.img)}" alt="" style="display:block;width:100%;max-height:340px;object-fit:cover;" onerror="this.parentNode.remove()"></div>` : ''}
    ${item.desc ? `<p style="font-size:14px;line-height:1.75;color:var(--text2);white-space:pre-wrap;margin:6px 0 14px;">${esc(item.desc)}</p>` : '<p style="font-size:13px;color:var(--text3);margin:6px 0 14px;">소개가 아직 준비 중입니다.</p>'}
    <button class="btn sm wide" style="width:100%;" onclick="document.getElementById('snuk-dyn-modal').classList.remove('open')">닫기</button>`, 560);
}

function makeContentCard(d, w, i) {
  const canApply = canApplyOf(d);
  const prep = isPreparing(d);
  const stTag = `<span class="tag ${tagClsOf(d)}">${esc(d.statusLabel)}</span>`;
  const kindTag = `<span class="tag t-neu">${d.kind === 'tournament' ? '대회' : d.trial ? '체험단' : '컨텐츠'}</span>`;
  const dd = canApply ? ddayOf(d.eventDate) : null;
  const click = cardClickOf(d);
  // 준비중은 모집 정보(인원·진행일)를 숨기고 내용만
  const spec = prep ? '' : [
    d.max > 0 ? `<dt>모집 인원</dt><dd>${d.filled}/${d.max}명</dd>` : '',
    d.eventDate ? `<dt>진행일</dt><dd>${esc(d.eventDate)}</dd>` : '',
  ].join('');
  const byline = d.adminMade === false
    ? `<span class="bava">✦</span><span>스트리머 컨텐츠</span>`
    : `<span class="bava">S</span><span>SNUK</span><span class="tag t-pri">공식</span>`;
  const btn = canApply
    ? `<button class="btn-w" onclick="event.stopPropagation();openApply('${d.kind}',${d.id})">신청하기</button>`
    : prep
      ? `<button class="btn-w ghost" onclick="event.stopPropagation();${click}">내용 보기</button>`
    : d.kind === 'tournament' && d.resultText
      ? `<button class="btn-w ghost" onclick="event.stopPropagation();showResult(${d.id})">결과 보기</button>`
      : `<button class="btn-w ghost">자세히 보기</button>`;
  const widthStyle = w ? `width:${w};min-width:${w};` : '';
  return `<div class="contcard" style="${widthStyle}" onclick="${click}">
    <span class="cthumb" style="background:${bgOf(i)};">
      ${d.img ? `<img src="${esc(d.img)}" alt="" onerror="this.remove()">` : ''}
      ${dd != null && dd >= 0 ? `<span class="dday${dd <= 3 ? ' urgent' : ''}">${dd === 0 ? 'D-DAY' : `D-${dd}`}</span>` : ''}
    </span>
    <div class="cbody">
      <div class="crow">${kindTag}${stTag}</div>
      <p class="ctitle">${esc(d.title)}</p>
      <p class="cintro">${esc(d.desc)}</p>
      ${spec ? `<dl class="cspec">${spec}</dl>` : ''}
      <div class="byline">${byline}</div>
      ${btn}
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
}

// ════════════════════════════════════════════
// 컨텐츠 페이지(/campaigns) — 상태 탭(모집중/진행중/종료) + 종류·정렬 + 그리드 (데모 "컨텐츠" 페이지 구성)
// ════════════════════════════════════════════
const CP_TABS = [
  ['open', '모집 중', (d) => d.status === 'open' || d.status === 'upcoming'],
  ['preparing', '준비 중', (d) => isPreparing(d)],
  ['ongoing', '진행 중', (d) => d.status === 'ongoing'],
  ['closed', '종료', (d) => d.status === 'closed'],
];
const CP_KINDS = [['all', '전체'], ['campaign', '컨텐츠'], ['trial', '체험단'], ['tournament', '대회']];
const CP_SORTS = [['reg', '등록순'], ['end', '마감순'], ['hot', '인기순']];
const cpState = { tab: 'open', kind: 'all', sort: 'reg' };
window.__cpSet = (k, v) => { cpState[k] = v; renderContentsPage(); };

function renderContentsPage() {
  const grid = document.getElementById('cp-grid');
  if (!grid) return;
  const all = [...(D().snukContents || []), ...(D().trialContents || []), ...(D().mugContents || [])];
  const pill = (cls, on, onclick, label) =>
    `<button class="${cls}${on ? ' on' : ''}" onclick="${onclick}">${label}</button>`;
  document.getElementById('cp-tabs').innerHTML = CP_TABS.map(([k, label, f]) =>
    pill('cp-tab', cpState.tab === k, `__cpSet('tab','${k}')`, `${label} <b>${all.filter(f).length}</b>`)).join('');
  document.getElementById('cp-kinds').innerHTML = CP_KINDS.map(([k, label]) =>
    pill('cp-pill', cpState.kind === k, `__cpSet('kind','${k}')`, label)).join('');
  document.getElementById('cp-sorts').innerHTML = CP_SORTS.map(([k, label]) =>
    pill('cp-pill sm', cpState.sort === k, `__cpSet('sort','${k}')`, label)).join('');

  const tab = CP_TABS.find(([k]) => k === cpState.tab) || CP_TABS[0];
  let items = all.filter(tab[2]);
  if (cpState.kind === 'trial') items = items.filter((d) => d.trial);
  else if (cpState.kind === 'campaign') items = items.filter((d) => d.kind === 'campaign' && !d.trial);
  else if (cpState.kind !== 'all') items = items.filter((d) => d.kind === cpState.kind);
  if (cpState.sort === 'end') {
    items = [...items].sort((a, b) => (a.applyEnd || a.eventDate || '9999').localeCompare(b.applyEnd || b.eventDate || '9999'));
  } else if (cpState.sort === 'hot') {
    items = [...items].sort((a, b) => (b.filled || 0) - (a.filled || 0));
  }
  const emptyMsg = { open: '모집 중인 컨텐츠가 없습니다. 곧 새로운 컨텐츠로 찾아올게요!', preparing: '준비 중인 컨텐츠가 없습니다.', ongoing: '진행 중인 컨텐츠가 없습니다.', closed: '종료된 컨텐츠가 없습니다.' };
  grid.innerHTML = items.length
    ? items.map((d, i) => makeContentCard(d, '', i)).join('')
    : emptyCard(emptyMsg[cpState.tab]);
}

// ════════════════════════════════════════════
// 홈 통합 빅그리드 — 캠페인 + 대회 큰 카드 (3열 슬라이드)
// ════════════════════════════════════════════

// ── 자동 슬라이드 타이머 공용(멱등 init 대비 키별 1개 유지, 호버 시 일시정지)
const _autoTimers = {};
function setAutoLoop(key, fn, ms) {
  if (_autoTimers[key]) clearInterval(_autoTimers[key]);
  _autoTimers[key] = setInterval(fn, ms);
}

// ── 라이브 배너 (히어로 아래, 어드민 on/off — 항목 13/18)

// ── SNUK 뉴스 (홈 섹션 — 항목 11)

// ── FEATURED 카드 (SNUK 컨텐츠/챔피언십 상단)
function renderFeatured(elId, d, tagText) {
  const el = document.getElementById(elId);
  if (!el) return;
  if (!d) { el.style.display = 'none'; return; }
  const canApply = canApplyOf(d);
  const badgeCls = badgeClsOf(d);
  el.style.display = '';
  el.setAttribute('onclick', d.kind === 'tournament' || canApply || isPreparing(d) ? cardClickOf(d) : '');
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
        ${!isPreparing(d) && d.max > 0 ? `<div class="featured-stat"><strong>${d.filled}/${d.max}명</strong>모집 현황</div>` : ''}
        ${!isPreparing(d) && d.eventDate ? `<div class="featured-stat"><strong>${esc(d.eventDate)}</strong>진행일</div>` : ''}
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
function openDynamicModal(innerHtml, width, fullPage) {
  let ov = document.getElementById('snuk-dyn-modal');
  if (!ov) {
    ov = document.createElement('div');
    ov.id = 'snuk-dyn-modal';
    ov.className = 'modal-overlay';
    ov.addEventListener('click', (e) => { if (e.target === ov) ov.classList.remove('open'); });
    (document.querySelector('.snuk-page') || document.body).appendChild(ov);
  }
  // fullPage: 작은 모달 대신 페이지처럼 전체 화면 전환 (스트리머 등록 등)
  ov.innerHTML = `<div class="modal${fullPage ? ' modal-fullpage' : ''}" style="${fullPage ? '' : `width:${width || '480px'};max-height:80vh;overflow-y:auto;`}">
    <button class="modal-close" onclick="document.getElementById('snuk-dyn-modal').classList.remove('open')">✕</button>
    ${fullPage ? `<div class="modal-fullpage-inner">
      <button class="btn btn-outline" style="margin-bottom:18px;padding:8px 16px;font-size:13px;"
        onclick="document.getElementById('snuk-dyn-modal').classList.remove('open')">← 돌아가기</button>
      ${innerHtml}</div>` : innerHtml}</div>`;
  ov.classList.add('open');
  if (fullPage) ov.scrollTop = 0;
}

// ════════════════════════════════════════════
// 스트리머 컨텐츠·대회 등록 (STREAMER+ — 백엔드가 등급·소유자 재검증)
// 공식(featured·큰 칸)은 관리자 전용, 스트리머 등록분은 작은 카드로만 노출.
// ════════════════════════════════════════════
const SP_INP = 'width:100%;margin-bottom:10px;padding:12px 14px;background:var(--bg3);border:1px solid var(--border);border-radius:8px;color:var(--text);font-size:14px;font-family:inherit;box-sizing:border-box;';
let _spEditId = null;
let _spRaw = null; // 수정 시 원본(미노출 필드 보존용 — update 는 전필드 전송이라 유실 방지)

// ── 신청 질문 빌더(네이버 폼 스타일) — 스트리머 등록/수정 모달용.
// 상태는 _spQs 배열, 텍스트 입력은 oninput 으로 상태만 갱신(재렌더 없음 — 포커스 유지),
// 구조 변경(유형/추가/삭제/순서)만 spQbRender 로 다시 그림.
let _spQs = [];
const SPQ_TYPES = [['SHORT', '단답형'], ['LONG', '장문형'], ['SELECT', '객관식 (하나)'], ['MULTI', '체크박스 (복수)']];
const SPQ_BTN = 'border:1px solid var(--border);background:var(--bg3);color:var(--text2);cursor:pointer;font-size:11px;padding:3px 8px;border-radius:6px;';

function spQbRender() {
  const box = document.getElementById('spc-qbuilder');
  if (!box) return;
  box.innerHTML = _spQs.map((q, i) => {
    const isChoice = q.type === 'SELECT' || q.type === 'MULTI';
    return `
    <div style="border:1px solid var(--border);border-radius:10px;padding:10px;margin-bottom:8px;background:var(--bg2);">
      <div style="display:flex;gap:6px;align-items:center;">
        <b style="font-size:11px;color:var(--accent);flex-shrink:0;">Q${i + 1}</b>
        <input value="${esc(q.q)}" placeholder="질문을 입력하세요" oninput="_spQs[${i}].q=this.value"
          style="flex:1;min-width:0;padding:7px 9px;border:1px solid var(--border);border-radius:7px;background:var(--bg3);color:var(--text);font-size:12.5px;font-family:inherit;">
        <select onchange="spQbType(${i},this.value)"
          style="flex-shrink:0;padding:7px 4px;border:1px solid var(--border);border-radius:7px;background:var(--bg3);color:var(--text2);font-size:11.5px;font-family:inherit;">
          ${SPQ_TYPES.map(([v, l]) => `<option value="${v}"${(q.type || 'SHORT') === v ? ' selected' : ''}>${l}</option>`).join('')}
        </select>
      </div>
      ${isChoice ? `<div style="margin-top:7px;padding-left:2px;">
        ${(q.options || []).map((o, oi) => `
          <div style="display:flex;gap:6px;align-items:center;margin-bottom:5px;">
            <span style="color:var(--text3);font-size:12px;flex-shrink:0;">${q.type === 'SELECT' ? '◯' : '☐'}</span>
            <input value="${esc(o)}" placeholder="선택지" oninput="_spQs[${i}].options[${oi}]=this.value"
              style="flex:1;min-width:0;padding:6px 8px;border:1px solid var(--border);border-radius:6px;background:var(--bg3);color:var(--text);font-size:12px;font-family:inherit;">
            <button type="button" onclick="spQbDelOpt(${i},${oi})" ${(q.options || []).length <= 1 ? 'disabled' : ''}
              style="border:none;background:none;color:var(--text3);cursor:pointer;font-size:11px;padding:3px 5px;">✕</button>
          </div>`).join('')}
        <button type="button" onclick="spQbAddOpt(${i})"
          style="border:none;background:none;color:var(--accent);cursor:pointer;font-size:11.5px;font-weight:700;padding:2px 4px;">＋ 선택지 추가</button>
      </div>` : `<div style="margin-top:6px;font-size:11px;color:var(--text3);">${q.type === 'LONG' ? '여러 줄 답변' : '한 줄 답변'} · 사진 첨부 가능</div>`}
      <div style="display:flex;align-items:center;gap:5px;margin-top:8px;border-top:1px solid var(--border);padding-top:7px;">
        <label style="display:flex;align-items:center;gap:4px;font-size:11.5px;font-weight:700;color:var(--text2);cursor:pointer;">
          <input type="checkbox" ${q.required ? 'checked' : ''} onchange="_spQs[${i}].required=this.checked"> 필수</label>
        <span style="flex:1;"></span>
        <button type="button" onclick="spQbMove(${i},-1)" ${i === 0 ? 'disabled' : ''} style="${SPQ_BTN}">↑</button>
        <button type="button" onclick="spQbMove(${i},1)" ${i === _spQs.length - 1 ? 'disabled' : ''} style="${SPQ_BTN}">↓</button>
        <button type="button" onclick="spQbDel(${i})" style="${SPQ_BTN}color:#e5484d;">삭제</button>
      </div>
    </div>`;
  }).join('')
  + `<button type="button" onclick="spQbAdd()"
      style="width:100%;border:1.5px dashed var(--border);background:none;color:var(--accent);cursor:pointer;padding:9px;border-radius:9px;font-size:12.5px;font-weight:700;margin-bottom:12px;">＋ 신청 질문 추가</button>`;
}
function spQbAdd() { _spQs.push({ q: '', required: true, type: 'SHORT', options: [] }); spQbRender(); }
function spQbDel(i) { _spQs.splice(i, 1); spQbRender(); }
function spQbMove(i, d) { const j = i + d; if (j < 0 || j >= _spQs.length) return; [_spQs[i], _spQs[j]] = [_spQs[j], _spQs[i]]; spQbRender(); }
function spQbType(i, t) { _spQs[i].type = t; if ((t === 'SELECT' || t === 'MULTI') && !(_spQs[i].options || []).length) _spQs[i].options = ['옵션 1']; spQbRender(); }
function spQbAddOpt(i) { (_spQs[i].options = _spQs[i].options || []).push('옵션 ' + (_spQs[i].options.length + 1)); spQbRender(); }
function spQbDelOpt(i, oi) { _spQs[i].options.splice(oi, 1); spQbRender(); }
/** 저장용 정리 — 빈 질문 제거, 객관식·체크박스는 빈 선택지 정리(선택지 0개면 질문 제외). */
function spQbCollect() {
  return _spQs
    .map((q) => {
      const choice = q.type === 'SELECT' || q.type === 'MULTI';
      return { q: (q.q || '').trim(), required: !!q.required, type: q.type || 'SHORT',
        options: choice ? (q.options || []).map((s) => s.trim()).filter(Boolean) : undefined };
    })
    .filter((q) => q.q && (!(q.type === 'SELECT' || q.type === 'MULTI') || q.options.length));
}

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
      <span class="badge ${badgeClsOf(x)}">${isPreparing(x) && x.ownerId && !spIsAdmin() ? '승인 대기' : esc(x.statusLabel)}</span>
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
    <textarea id="spc-desc" style="${SP_INP}resize:vertical;" rows="6" placeholder="설명"></textarea>
    <div style="font-size:12px;font-weight:700;color:var(--text2);margin:2px 0 7px;">${isT ? '참가 ' : ''}신청 질문
      <span style="font-weight:400;color:var(--text3);">— 신청자가 신청할 때 답변합니다</span></div>
    <div id="spc-qbuilder"></div>
    <div style="display:flex;gap:8px;">
      <input id="spc-date" type="date" style="${SP_INP}flex:1;" title="진행일">
      <input id="spc-cap" type="number" min="0" style="${SP_INP}flex:1;" placeholder="${isT ? '정원(명)' : '모집 인원'}">
      ${spStatusSelect('PREPARING')}
    </div>
    ${spIsAdmin() ? '' : `<div style="font-size:11.5px;line-height:1.6;color:var(--text3);margin:-6px 0 10px;">등록하면 <b>준비중</b>(내용만 공개)으로 올라가고, 관리자가 승인하면 <b>모집중</b>으로 바뀌어 신청을 받을 수 있어요.</div>`}
    <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px;">
      <input id="spc-img" type="file" accept="image/*" style="font-size:12px;flex:1;color:var(--text2);">
      <img id="spc-img-prev" alt="" style="display:none;width:56px;height:36px;object-fit:cover;border-radius:6px;">
    </div>
    <button id="spc-submit" class="btn-apply" style="width:100%;padding:13px;font-size:15px;" onclick="spSubmit('${kind}')">등록하기</button>`, null, true);
  _spQs = [];
  spQbRender();
}

// 스트리머 등록분은 승인제 — 비관리자는 모집중/오픈예정을 직접 고를 수 없다(서버도 M008로 차단). 이미 승인된 상태는 그대로 유지 가능.
function spIsAdmin() { return (window.__snukMe || {}).role === 'ADMIN'; }
function spStatusSelect(current) {
  const opts = spIsAdmin()
    ? [['PREPARING', '준비중'], ['SCHEDULED', '오픈예정'], ['OPEN', '모집중'], ['ONGOING', '진행중 (모집 마감)'], ['CLOSED', '종료']]
    : [['PREPARING', '준비중 (관리자 승인 대기)'], ['ONGOING', '진행중 (모집 마감)'], ['CLOSED', '종료']];
  if (!spIsAdmin() && (current === 'OPEN' || current === 'SCHEDULED')) {
    opts.splice(1, 0, [current, current === 'OPEN' ? '모집중 (승인됨)' : '오픈예정 (승인됨)']);
  }
  return `<select id="spc-status" style="${SP_INP}flex:1;">${opts.map(([v, l]) =>
    `<option value="${v}"${v === current ? ' selected' : ''}>${l}</option>`).join('')}</select>`;
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
    _spQs = (d.applyQuestions || []).map((q) => ({
      q: q.q, required: q.required, type: q.type || 'SHORT', options: (q.options || []).slice(),
    }));
    spQbRender();
    const sel = document.getElementById('spc-status');
    // 대회 enum(CLOSED=모집 마감·진행, DONE=종료)을 공용 셀렉트(ONGOING/CLOSED) 표기로 변환
    const shown = kind === 'tournament' ? ({ CLOSED: 'ONGOING', DONE: 'CLOSED' }[d.status] || d.status) : d.status;
    sel.outerHTML = spStatusSelect(['PREPARING', 'OPEN', 'SCHEDULED', 'ONGOING', 'CLOSED'].includes(shown) ? shown : 'CLOSED');
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
        ${(p.answers || []).map((a, i) => `<div style="font-size:11px;color:var(--text2);margin-top:2px;"><b>Q${i + 1}.</b> ${esc((a && a.text) || '')}${a && a.imageUrl ? ` <a href="${esc(a.imageUrl)}" target="_blank" rel="noopener" style="color:var(--accent);">[사진 보기]</a>` : ''}</div>`).join('')}
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
    ${list.length ? `<button class="btn btn-outline" style="width:100%;margin-top:14px;padding:10px;" onclick="spExportCsv(${tourId})">📄 신청 내역 엑셀(CSV) 다운로드</button>` : ''}
    <button class="btn btn-outline" style="width:100%;margin-top:8px;padding:10px;" onclick="openStreamerPost('tournament')">← 내 대회 목록으로</button>`, null, true);
}

async function spExportCsv(tourId) {
  try {
    await A().exportParticipants(tourId);
  } catch (e) {
    showToast('다운로드에 실패했습니다');
  }
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
    const picked = v('spc-status');
    const status = kind === 'tournament' ? ({ ONGOING: 'CLOSED', CLOSED: 'DONE' }[picked] || picked) : picked;
    const questions = spQbCollect();
    const body = kind === 'tournament'
      ? { title, gameName: game, description: desc, bannerImageUrl: img,
          detailImageUrl: _spRaw ? _spRaw.detailImageUrl : null,
          eventDate: date, applyStart: _spRaw ? _spRaw.applyStart : null, applyEnd: _spRaw ? _spRaw.applyEnd : null,
          capacity: cap, status, resultText: _spRaw ? _spRaw.resultText : null, applyQuestions: questions,
          featured: _spRaw ? _spRaw.featured : false, sortOrder: _spRaw ? _spRaw.sortOrder : 0 }
      : { title, gameName: game, description: desc, promoImageUrl: img,
          eventDate: date, applyStart: _spRaw ? _spRaw.applyStart : null, applyEnd: _spRaw ? _spRaw.applyEnd : null,
          status, distributionType: _spRaw ? _spRaw.distributionType : 'APPROVAL',
          keyMode: _spRaw ? _spRaw.keyMode : 'QUANTITY', totalSlots: cap, applyQuestions: questions,
          featured: _spRaw ? _spRaw.featured : false, sortOrder: _spRaw ? _spRaw.sortOrder : 0 };
    if (_spEditId) await A().updateContent(kind, _spEditId, body);
    else await A().createContent(kind, body);
    showToast(_spEditId ? '✅ 수정됐습니다' : spIsAdmin() ? '✅ 등록됐습니다!' : '✅ 등록됐어요 — 관리자 승인 후 모집이 시작됩니다');
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
  let item = list.find((x) => x.id === id);
  // 게임체험단 연계 캠페인은 snukContents 에서 제외되어 있음 → games 에서 찾음
  // (홈 "모집 중인 체험단" 카드 신청 무반응 픽스)
  if (!item && kind === 'campaign') {
    const g = (D().games || []).find((x) => x.campaignId === id);
    if (g) item = { id, title: g.name, applyQuestions: g.applyQuestions || [] };
  }
  if (!item) return;
  if (!window.__snukLoggedIn) { showToast('로그인 후 신청할 수 있습니다'); openLogin(); return; }
  const questions = item.applyQuestions || [];
  pendingApply = { kind, id, title: item.title, questions };
  const sub = document.getElementById('apply-modal-sub');
  if (sub) sub.textContent = `"${item.title}" ${kind === 'tournament' ? '대회 참가' : '컨텐츠'} 신청`;
  const qs = document.getElementById('apply-modal-qs');
  if (qs) {
    qs.style.display = questions.length ? '' : 'none';
    // 유형별 답변 입력(네이버 폼 스타일): 단답/장문/객관식(라디오)/체크박스
    const AQ_INP = 'display:block;width:100%;margin-top:6px;padding:9px 11px;border:1px solid var(--border);border-radius:8px;background:var(--bg2);color:var(--text);font-size:13px;font-family:inherit;box-sizing:border-box;';
    qs.innerHTML = questions.map((q, i) => {
      const t = q.type || 'SHORT';
      let field;
      if (t === 'SELECT' || t === 'MULTI') {
        field = (q.options || []).map((o) => `
          <label style="display:flex;align-items:center;gap:8px;margin-top:6px;padding:8px 11px;border:1px solid var(--border);border-radius:8px;background:var(--bg2);color:var(--text);font-size:13px;font-weight:500;cursor:pointer;">
            <input type="${t === 'SELECT' ? 'radio' : 'checkbox'}" name="apply-q-${i}" value="${esc(o)}" style="accent-color:var(--accent);flex-shrink:0;"> ${esc(o)}
          </label>`).join('');
      } else if (t === 'SHORT') {
        field = `<input id="apply-q-${i}" type="text" placeholder="답변을 입력하세요" style="${AQ_INP}">`;
      } else {
        field = `<textarea id="apply-q-${i}" rows="3" placeholder="답변을 입력하세요" style="${AQ_INP}resize:vertical;"></textarea>`;
      }
      return `<div style="display:block;margin-bottom:12px;font-size:12.5px;font-weight:700;color:var(--text);">
        Q${i + 1}. ${esc(q.q)} <span style="font-weight:400;color:var(--text3);">${q.required ? '(필수)' : '(선택)'}</span>${field}</div>`;
    }).join('');
  }
  document.getElementById('apply-modal').classList.add('open');
}

async function confirmApply() {
  if (!pendingApply) return;
  const { kind, id, questions } = pendingApply;
  // 질문 답변 수집 — 필수 미입력이면 신청 차단(백엔드도 재검증)
  let answers = null;
  if (questions && questions.length) {
    answers = questions.map((q, i) => {
      const t = q.type || 'SHORT';
      let text;
      if (t === 'SELECT' || t === 'MULTI') {
        text = Array.from(document.querySelectorAll(`[name="apply-q-${i}"]:checked`)).map((el) => el.value).join(', ');
      } else {
        const el = document.getElementById(`apply-q-${i}`);
        text = el ? el.value.trim() : '';
      }
      return { text: text || null, imageUrl: null };
    });
    const missing = questions.findIndex((q, i) => q.required && !answers[i].text);
    if (missing >= 0) {
      showToast(`필수 질문에 답변해주세요: Q${missing + 1}`);
      const el = document.getElementById(`apply-q-${missing}`);
      if (el) el.focus();
      return;
    }
  }
  const btn = document.getElementById('apply-confirm-btn');
  if (btn) { btn.disabled = true; btn.textContent = '신청 중...'; }
  try {
    if (kind === 'tournament') {
      await A().applyTournament(id, answers || undefined);
      showToast('✅ 참가 신청이 접수됐습니다! (운영자 승인 후 확정)');
    } else {
      const res = await A().applyCampaign(id, answers || undefined);
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
    const prep = !!g.preparing;
    const stLabel = canApply ? '모집중' : prep ? '준비중' : '마감';
    const stCls = canApply ? 'badge open' : prep ? 'badge preparing' : 'badge closed';
    return `<div class="game-card" style="width:${cardWidth(3, 1)};min-width:${cardWidth(3, 1)};">
      <div class="game-thumb" style="position:relative;background:${bgOf(i)};">
        ${g.img ? `<img src="${esc(g.img)}" alt="${esc(g.name)}" onerror="this.remove()">` : `<div style="font-size:64px;opacity:.4;">${esc(g.name.slice(0, 1))}</div>`}
        <div class="game-thumb-grad"></div>
        <div style="position:absolute;top:12px;left:12px;z-index:1;">
          <span class="${stCls}">${stLabel}</span>
        </div>
        <div class="game-thumb-caption">
          <div class="game-name">${esc(g.name)}</div>
          ${g.publisher ? `<div class="game-publisher">${esc(g.publisher)}</div>` : ''}
        </div>
      </div>
      <div class="game-body">
        <div class="game-desc">${esc(g.desc)}</div>
        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:0;">
          <span style="font-size:11px;color:var(--text3);">${!prep && g.max > 0 ? `신청 ${g.members}/${g.max}` : prep ? '모집 시작 전' : ''}</span>
          <div style="display:flex;gap:6px;">
            ${g.gameLinkUrl ? `<a href="${esc(g.gameLinkUrl)}" target="_blank" rel="noopener"><button class="btn btn-outline" style="padding:7px 12px;font-size:11px;">게임 링크 ↗</button></a>` : ''}
            <button class="btn-apply" ${canApply ? `onclick="openGame(${i})"` : 'style="background:var(--bg4);color:var(--text3);cursor:default;"'}>${canApply ? '신청하기' : stLabel}</button>
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
  // 신청 질문이 있으면 공용 신청 모달(질문 입력)로 넘김
  if (g.applyQuestions && g.applyQuestions.length) {
    closeModal('game-modal');
    openApply('campaign', g.campaignId);
    return;
  }
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

const platColor = { chz: '#00c73c', soop: '#34c7ff', yt: '#ff4040', cime: '#7c5cff' };
const platShort = { chz: '치', soop: '숲', yt: 'YT', cime: '씨' };
const platLabel = { chz: '치지직', soop: '숲', yt: '유튜브', cime: '씨미' };

// ── 스트리머 채널 섹션 (실 스트리머 등급 회원)
let streamerChanPos = 0;

let _streamerQuery = '';
function filterStreamerChannels(q) {
  // 공백 무시 — "혈 액"/"혈액" 둘 다 매칭
  _streamerQuery = String(q == null ? '' : q).toLowerCase().replace(/\s+/g, '');
  initStreamerChannels();
}

function initStreamerChannels() {
  const track = document.getElementById('streamer-ch-track');
  if (!track) return;
  let streamers = D().streamers || [];
  if (_streamerQuery) {
    streamers = streamers.filter((s) => String(s.name || '').toLowerCase().replace(/\s+/g, '').includes(_streamerQuery));
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
// FILTERS
// ════════════════════════════════════════════
function filterMug(s, btn) {
  document.querySelectorAll('#mug-tabs .tab').forEach((t) => t.classList.remove('active')); btn.classList.add('active');
  const all = D().mugContents || [];
  const f = s === 'all' ? all : all.filter((d) => d.status === s);
  initSlider('mug-slider', f.length ? f.map((d, i) => makeContentCard(d, cardWidth(6), i)).join('') : emptyCard('해당 상태의 대회가 없습니다.'));
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
// 알림함 (V23 — 실 API)
// ════════════════════════════════════════════
const NOTIF_ICON = {
  APPLICATION_APPROVED: '✅', APPLICATION_REJECTED: '❌',
  TOURNAMENT_APPROVED: '🏆', TOURNAMENT_REJECTED: '❌',
  ROLE_APPROVED: '🎉', ROLE_REJECTED: '📮', SONG_REQUESTED: '🎵',
};
async function openNotifications() {
  if (!window.__snukLoggedIn) { openLogin(); return; }
  let data;
  try {
    data = await A().notifications();
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '알림을 불러오지 못했습니다');
    return;
  }
  const rows = (data.items || []).map((n) => `
    <button style="display:flex;gap:10px;width:100%;text-align:left;background:${n.read ? 'transparent' : 'var(--bg3)'};border:none;border-bottom:1px solid var(--border);padding:11px 6px;cursor:${n.linkPath ? 'pointer' : 'default'};color:var(--text);"
      ${n.linkPath ? `onclick="document.getElementById('snuk-dyn-modal').classList.remove('open');window.__snukNav('${esc(n.linkPath)}')"` : ''}>
      <span style="font-size:16px;flex-shrink:0;">${NOTIF_ICON[n.type] || '🔔'}</span>
      <span style="flex:1;min-width:0;">
        <span style="display:block;font-size:13px;font-weight:${n.read ? '500' : '700'};">${esc(n.title)}</span>
        ${n.body ? `<span style="display:block;font-size:12px;color:var(--text3);margin-top:2px;">${esc(n.body)}</span>` : ''}
        <span style="display:block;font-size:11px;color:var(--text3);margin-top:3px;">${esc((n.createdAt || '').slice(0, 16).replace('T', ' '))}</span>
      </span>
      ${n.read ? '' : '<span style="width:7px;height:7px;border-radius:50%;background:var(--accent);flex-shrink:0;margin-top:5px;"></span>'}
    </button>`).join('');
  openDynamicModal(`
    <div class="modal-title">🔔 알림</div>
    <div class="modal-sub">안 읽은 알림 ${data.unreadCount || 0}개</div>
    <div style="max-height:52vh;overflow-y:auto;margin:0 -6px;">${rows || '<div style="padding:26px 6px;font-size:13px;color:var(--text3);text-align:center;">알림이 없습니다.</div>'}</div>
    ${(data.items || []).length ? `<button class="btn btn-outline" style="width:100%;margin-top:14px;padding:10px;" onclick="markAllNotifsRead(this)">모두 읽음 처리</button>` : ''}`);
}
async function markAllNotifsRead(btn) {
  try {
    await A().readAllNotifications();
    if (btn) btn.textContent = '✓ 모두 읽음 처리했습니다';
  } catch (e) {
    showToast('처리에 실패했습니다');
  }
}

// ════════════════════════════════════════════
// 스트리머 권한 신청 (V23 — VIEWER 전용, 실 API)
// ════════════════════════════════════════════
const ROLE_REQ_LABEL = { PENDING: '⏳ 운영자 확인 중', APPROVED: '✅ 승인됨', REJECTED: '반려됨' };
async function openRoleRequest() {
  if (!window.__snukLoggedIn) { openLogin(); return; }
  let mine = null;
  try {
    mine = await A().myRoleRequest();
  } catch (e) { /* 조회 실패 시 신규 신청 폼으로 */ }
  const statusHtml = mine
    ? `<div style="background:var(--bg3);border-radius:10px;padding:12px 14px;margin-bottom:14px;font-size:12.5px;line-height:1.7;">
        최근 신청: <b>${ROLE_REQ_LABEL[mine.status] || mine.status}</b> · ${esc((mine.createdAt || '').slice(0, 10))}
        ${mine.status === 'PENDING' ? '<br>운영자 확인 후 알림으로 결과를 보내드려요.' : ''}
        ${mine.status === 'REJECTED' ? '<br>아래에서 다시 신청할 수 있어요.' : ''}
      </div>` : '';
  const canApply = !mine || mine.status === 'REJECTED';
  openDynamicModal(`
    <div class="modal-title">🛡️ 스트리머 권한 신청</div>
    <div class="modal-sub">승인되면 컨텐츠·체험단·대회에 신청할 수 있어요</div>
    ${statusHtml}
    ${canApply ? `
      <textarea id="role-req-msg" rows="4" placeholder="활동 중인 채널, 방송 분야 등 운영자가 참고할 내용을 적어주세요 (선택)"
        style="width:100%;padding:11px 12px;border:1px solid var(--border);border-radius:10px;background:var(--bg3);color:var(--text);font-size:13px;font-family:inherit;resize:vertical;margin-bottom:14px;"></textarea>
      <button id="role-req-btn" class="btn-apply" style="width:100%;padding:11px;font-size:13px;border-radius:8px;" onclick="submitRoleRequest()">신청하기</button>`
    : ''}`);
}
async function submitRoleRequest() {
  const btn = document.getElementById('role-req-btn');
  if (btn) { btn.disabled = true; btn.textContent = '신청 중...'; }
  try {
    const msg = (document.getElementById('role-req-msg') || {}).value || '';
    await A().applyRoleRequest(msg.trim());
    document.getElementById('snuk-dyn-modal').classList.remove('open');
    showToast('✅ 권한 신청이 접수됐습니다! 운영자 확인 후 알림으로 알려드려요.');
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '신청에 실패했습니다');
    if (btn) { btn.disabled = false; btn.textContent = '신청하기'; }
  }
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
  const spDate = (document.getElementById('sp-date') || {}).value || '';
  const spTime = (document.getElementById('sp-time') || {}).value || '';
  const scheduledAt = spDate ? `${spDate}T${spTime || '00:00'}:00` : null;
  try {
    await A().createSpotlight({ title: title.trim(), platform: platformMap[selectedPlatform] || 'CHZZK', streamUrl: url.trim(), scheduledAt });
    showToast('✅ 스포트라이트가 등록됐습니다! (2시간 노출)');
    closeModal('spotlight-modal');
    if (A().reloadData) A().reloadData();
  } catch (e) {
    showToast(A().errorMessage ? A().errorMessage(e) : '등록에 실패했습니다 (스트리머만 등록 가능)');
  }
}

function openSpotlight() {
  if (!window.__snukLoggedIn) { showToast('로그인 후 등록할 수 있습니다'); openLogin(); return; }
  // 포인트 비용 안내(항목 9) — 하루 첫 로그인 적립 포인트로 등록
  const costLine = document.getElementById('sp-cost-line');
  if (costLine) {
    const cost = parseInt(((D() && D().siteSettings) || {}).SPOTLIGHT_POINT_COST || '50', 10) || 0;
    const mine = (window.__snukMe && window.__snukMe.points) || 0;
    costLine.textContent = cost > 0
      ? `등록 시 ${cost}P 차감 (내 포인트: ${mine}P — 매일 첫 로그인마다 적립)`
      : '지금은 무료로 등록할 수 있어요.';
  }
  document.getElementById('spotlight-modal').classList.add('open');
}

// ════════════════════════════════════════════
// 검색 (실데이터 통합 검색)
// ════════════════════════════════════════════
// 공백 무시 정규화 — "혈 액" 으로도 "혈액" 이 검색되게 (질의·대상 양쪽 적용)
const searchNorm = (s) => String(s == null ? '' : s).toLowerCase().replace(/\s+/g, '');
function snukSearch(q) {
  const query = searchNorm(q);
  if (!query) return;
  const d = D();
  const results = [];
  const hit = (v) => searchNorm(v).includes(query);
  (d.streamers || []).forEach((x) => { if (hit(x.name)) results.push({ label: x.name, cat: '스트리머', path: `/streamers/${x.id}` }); });
  (d.snukContents || []).forEach((x) => { if (hit(x.title)) results.push({ label: x.title, cat: 'SNUK 컨텐츠', path: '/campaigns' }); });
  (d.mugContents || []).forEach((x) => { if (hit(x.title)) results.push({ label: x.title, cat: '대회', path: '/championship' }); });
  (d.games || []).forEach((x) => { if (hit(x.name)) results.push({ label: x.name, cat: '게임체험단', path: '/campaigns' }); });
  (d.goods || []).forEach((x) => { if (hit(x.name)) results.push({ label: x.name, cat: '굿즈', path: '/goods' }); });
  (d.videos || []).forEach((x) => { if (hit(x.title)) results.push({ label: x.title, cat: '영상', path: '/videos' }); });
  (d.news || []).forEach((x) => { if (hit(x.title)) results.push({ label: x.title, cat: '뉴스', path: `/news/${x.id}` }); });

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
    initGameTrial();
    initGameVideos();
    if (window._rerenderGoods) window._rerenderGoods();
    initPartners();
    streamerChanPos = 0;
    initStreamerChannels();
    Object.keys(sliderPos).forEach((k) => { sliderPos[k] = 0; const t = document.getElementById(k === 'game-slider' ? 'game-grid' : k); if (t) t.style.transform = 'translateX(0)'; });
  }, 200);
});

// ════════════════════════════════════════════
// 홈 — 데모 시안 구성 (#home-demo)
// 광고 배너 / 라이브 스테이지 / 스트리머 스토리 / 모집 컨텐츠·체험단 /
// 체험단 후기 / 커뮤니티·뉴스 / 방송도우미 소스
// 전부 실데이터(window.__SNUK_DATA). 없으면 빈 상태 문구.
// ════════════════════════════════════════════
const dhEmpty = (msg) => `<div class="dh-empty">${esc(msg)}</div>`;
const AVA_BG = ['#6C4BE6', '#E0714A', '#C79212', '#D14A86', '#3F82C4', '#0E9F55'];
function dhAva(name, img, cls) {
  const i = String(name || '?').charCodeAt(0) % AVA_BG.length;
  const inner = img ? `<img src="${esc(img)}" alt="" onerror="this.remove()">` : esc(String(name || '?').slice(0, 1));
  return `<span class="ava ${cls || ''}" style="background:${AVA_BG[i]}">${inner}</span>`;
}
const CHZZK_ID_RE = /chzzk\.naver\.com\/(?:live\/)?([0-9a-f]{16,40})/i;
function chzzkIdOf(url) {
  const m = String(url || '').match(CHZZK_ID_RE);
  return m ? m[1] : null;
}
function dhDday(dateStr) {
  if (!dateStr) return null;
  const d = Math.ceil((new Date(`${dateStr}T23:59:59`).getTime() - Date.now()) / 86400000);
  return Number.isFinite(d) ? d : null;
}

// ── 상단 AD 배너 — 데모 #homeBoard (이미지 + AD 칩 + 점, 6초 자동 전환)
// 슬라이드 = 어드민이 등록한 배너 이미지들(히어로 + 페이지 배너). 미설정값은 '-' 또는 'none'.
const AD_BG = ['linear-gradient(120deg,#E0714A,#C9924A)', 'linear-gradient(120deg,#4B2E8F,#7B4BD8)',
  'linear-gradient(120deg,#0E7A55,#2BC26A)', 'linear-gradient(120deg,#2E4C8F,#3F82C4)'];
const isSet = (v) => { const s = String(v ?? '').trim(); return !!s && s !== '-' && s !== 'none'; };
let _dhAd = 0;
// 슬라이드 = { img, link } — 어드민 "광고 슬롯"(이미지+링크, 노출 중인 것만)이 있으면 그것만,
// 하나도 없으면 예전처럼 사이트 이미지(히어로/페이지 배너)를 돌린다(링크 없음 → 컨텐츠 페이지).
function dhAdSlides() {
  const ads = (D().ads || []).filter((a) => a && a.img);
  if (ads.length) return ads.map((a) => ({ img: a.img, link: a.link || null, title: a.title || '' }));
  const ss = D().siteSettings || {};
  const keys = ['HERO_IMAGE_URL', 'BANNER_CONTENTS_URL', 'BANNER_GAMES_URL', 'BANNER_CHAMPIONSHIP_URL',
    'BANNER_STREAMERS_URL', 'BANNER_LIVE_URL', 'BANNER_VIDEOS_URL'];
  return keys.filter((k) => isSet(ss[k])).map((k) => ({ img: ss[k].trim(), link: null, title: '' }));
}
// 광고 클릭: 외부 https → 새 탭, 내부 경로 → SPA 이동, 링크 없음 → 컨텐츠 페이지
function dhAdGo(slide) {
  const link = slide && slide.link;
  if (!link) { window.__snukNav('/campaigns'); return; }
  if (/^https?:\/\//i.test(link)) { window.open(link, '_blank', 'noopener'); return; }
  window.__snukNav(link);
}
function dhBoard() {
  const el = document.getElementById('dh-board');
  if (!el) return;
  const slides = dhAdSlides();
  const n = Math.max(1, slides.length);
  if (_dhAd >= n) _dhAd = 0;
  el.innerHTML = `<span class="boardimg">
      ${slides.length ? `<img src="${esc(slides[_dhAd].img)}" alt="${esc(slides[_dhAd].title || '')}" onerror="this.remove()">` : ''}
      ${n > 1 ? `<span class="dots boarddots">${slides.map((_, i) =>
        `<button class="dot${i === _dhAd ? ' on' : ''}" onclick="event.stopPropagation();dhPickAd(${i})"></button>`).join('')}</span>` : ''}
    </span>
    <span class="adlabel">AD</span>`;
  el.style.background = slides.length ? '' : AD_BG[_dhAd % AD_BG.length];
  el.onclick = () => dhAdGo(dhAdSlides()[_dhAd]);
  el.style.cursor = 'pointer';
  if (n > 1) setAutoLoop('dhAd', () => { _dhAd = (_dhAd + 1) % n; dhPaintAd(); }, 6000);
}
// 데모 paintAds — 전체 재렌더 없이 이미지/점만 갈아끼운다
function dhPaintAd() {
  const el = document.getElementById('dh-board');
  if (!el) return;
  const slides = dhAdSlides();
  if (!slides.length) return;
  const img = el.querySelector('img');
  if (img) { img.src = slides[_dhAd].img; img.alt = slides[_dhAd].title || ''; }
  el.querySelectorAll('.dot').forEach((d, i) => d.classList.toggle('on', i === _dhAd));
}
function dhPickAd(i) { _dhAd = i; dhPaintAd(); }

// ── 라이브: 스포트라이트(홍보 중) + 방송 중 스트리머
let _dhLiveOn = 0;
function dhLiveList() {
  const spots = (D().spotlights || []).map((s) => ({
    name: s.name, title: s.sub || '스포트라이트 방송', img: s.img, url: s.url,
    platform: s.platform, spot: true, chzzk: chzzkIdOf(s.url),
  }));
  const seen = new Set(spots.map((s) => s.name));
  const lives = (D().streamers || []).filter((s) => s.live && !seen.has(s.name)).map((s) => ({
    name: s.name, title: s.liveTitle || '방송 중', img: s.img, url: s.channelUrl,
    platform: s.platform === 'soop' ? 'soop' : 'chz', spot: false,
    chzzk: chzzkIdOf(s.channelUrl), streamerId: s.id,
  }));
  return [...spots, ...lives];
}

// 스눅 공식 채널(LIVE_CHANNEL_ID) 이 방송 중이면 라이브 스테이지에 노출 — 세션당 1회만 조회
let _dhOfficial = null;
function dhOfficialLive() {
  const ch = (D().chzzkChannelId || '').trim();
  if (!ch || ch === '-') return;
  if (_dhOfficial === false) return;
  if (_dhOfficial) { dhRenderOfficial(_dhOfficial, ch); return; }
  fetch('/api/live/status').then((r) => r.json()).then((s) => {
    if (!s || !s.live) { _dhOfficial = false; return; }
    _dhOfficial = { title: s.liveTitle || '스눅 공식 방송' };
    dhRenderOfficial(_dhOfficial, ch);
  }).catch(() => { _dhOfficial = false; });
}
function dhRenderOfficial(info, ch) {
  const wrap = document.getElementById('dh-livewrap');
  const stage = document.getElementById('dh-stage');
  const nl = document.getElementById('dh-nowlist');
  if (!wrap || !stage) return;
  wrap.style.display = '';
  stage.innerHTML = `<div class="lvplayer" id="dh-player"></div>
    <span class="lvbadge">LIVE</span>
    <span class="lvcnt" id="dh-lvcnt" style="display:none"></span>
    <div class="lvover">${dhAva('SNUK')}
      <div class="lvmeta2"><p class="lvtitle">${esc(info.title)}</p>
      <p class="dh-row"><span class="micro">SNUK 공식</span></p></div>
    </div>
    <button class="lvopen" onclick="event.stopPropagation();window.open('https://chzzk.naver.com/live/${esc(ch)}','_blank')">방송 보러가기 →</button>`;
  dhMountPlayer(stage, ch, 'SNUK 공식');
  if (nl) {
    nl.innerHTML = `<button class="nowrow on">
      <span class="nowth"><span class="nl">LIVE</span></span>
      <span class="nowmeta"><span class="nowt">${esc(info.title)}</span><span class="nown">SNUK 공식</span></span>
    </button>`;
  }
}

function dhStage() {
  const stage = document.getElementById('dh-stage');
  const wrap = document.getElementById('dh-livewrap');
  if (!stage || !wrap) return;
  dhDestroyPlayer(); // 무대 재렌더 — 이전 스트림 정리
  stage.classList.remove('playing');
  const list = dhLiveList();
  if (!list.length) {
    // 데모(메인 화면)와 동일하게 방송이 없어도 무대 자리는 유지 — 대기 화면 노출.
    // (숨기면 스트리머 줄이 상단으로 올라와 데모 구성과 달라 보임 — 뮤마랭 08-14 피드백)
    wrap.style.display = '';
    stage.innerHTML = `<span class="lvposter"><span class="em">📺</span>지금은 방송 준비 중입니다
      <span style="display:block;margin-top:8px;font-size:12.5px;color:var(--text3);font-weight:400;">스트리머가 방송을 켜면 이 자리에서 바로 재생됩니다</span></span>
      <span class="lvbadge" style="background:var(--bg4);color:var(--text3);">대기 중</span>`;
    const nlEmpty = document.getElementById('dh-nowlist');
    if (nlEmpty) nlEmpty.innerHTML = '<div style="padding:26px 16px;font-size:13px;color:var(--text3);text-align:center;">방송 중인 스트리머가 없습니다</div>';
    const ls = document.querySelector('#home-demo .livesec');
    if (ls) ls.style.display = 'none';
    // 홍보/스트리머 방송이 없어도 스눅 공식 채널이 켜져 있으면 그걸 무대에 건다
    dhOfficialLive();
    return;
  }
  wrap.style.display = '';
  if (_dhLiveOn >= list.length) _dhLiveOn = 0;
  const cur = list[_dhLiveOn];
  // 치지직 = 인페이지 HLS 플레이어(window.__snukPlayer, src/snuk/chzzkPlayer.ts). 클릭=소리, 더블클릭=전체화면.
  // (예전 전체 페이지 크롭 임베드는 휠 드리프트 때문에 pointer-events:none → 클릭이 아예 안 됨(08-18 피드백) → 교체.
  //  재생주소를 못 받는 19금/비공개만 크롭 임베드로 폴백 — dhMountPlayer 참고)
  const player = cur.chzzk
    ? `<div class="lvplayer" id="dh-player"></div>`
    : `<span class="lvposter"><span class="em">📺</span>${esc(cur.name)} 님이 방송 중입니다</span>`;
  stage.innerHTML = `${player}
    <span class="lvbadge">LIVE</span>
    <span class="lvcnt" id="dh-lvcnt" style="display:none"></span>
    <div class="lvover">
      ${dhAva(cur.name, cur.img)}
      <div class="lvmeta2">
        <p class="lvtitle">${esc(cur.title)}</p>
        <p class="dh-row"><span class="micro">${esc(cur.name)}</span>
          ${cur.spot ? '<span class="lvtag">스포트라이트</span>' : ''}</p>
      </div>
    </div>
    ${cur.url ? `<button class="lvopen" onclick="event.stopPropagation();window.open('${esc(cur.url)}','_blank')">방송 보러가기 →</button>` : ''}`;
  if (cur.chzzk) dhMountPlayer(stage, cur.chzzk, cur.name);

  const rows = list.map((x, i) => `<button class="nowrow${i === _dhLiveOn ? ' on' : ''}" onclick="dhPickLive(${i})">
      <span class="nowth">${x.img ? `<img src="${esc(x.img)}" alt="" onerror="this.remove()">` : ''}<span class="nl">LIVE</span></span>
      <span class="nowmeta"><span class="nowt">${esc(x.title)}</span><span class="nown">${esc(x.name)}</span></span>
      ${x.spot ? '<span class="nowv">홍보중</span>' : ''}
    </button>`).join('');
  const nl = document.getElementById('dh-nowlist');
  if (nl) nl.innerHTML = rows;

  const lr = document.getElementById('dh-liverow');
  if (lr) {
    lr.innerHTML = list.map((x, i) => `<button class="livecard" onclick="dhPickLive(${i})">
      <span class="livethumb">${x.img ? `<img src="${esc(x.img)}" alt="" onerror="this.remove()">` : ''}
        <span class="lvbadge" style="left:8px;top:8px;font-size:10px;padding:3px 8px;">LIVE</span></span>
      <span class="livebody"><span class="lvt">${esc(x.title)}</span>
        <span class="dh-row" style="margin-top:6px">${dhAva(x.name, x.img)}<span class="lvn">${esc(x.name)}</span></span></span>
    </button>`).join('');
  }
}
// 무대에 치지직 HLS 플레이어 장착. 이전 플레이어는 파괴(무대 재렌더/채널 전환 시 스트림 누수 방지).
let _dhPlayer = null;
let _dhPlayerWatch = 0;
function dhDestroyPlayer() {
  if (_dhPlayer) { try { _dhPlayer.destroy(); } catch (e) { /* noop */ } _dhPlayer = null; }
  if (_dhPlayerWatch) { clearInterval(_dhPlayerWatch); _dhPlayerWatch = 0; }
}
function dhMountPlayer(stage, channelId, name) {
  const host = document.getElementById('dh-player');
  if (!host) return;
  dhDestroyPlayer();
  if (!window.__snukPlayer) { dhFallbackCrop(stage, channelId, name); return; }
  // 홈을 떠나면(SPA 라우팅으로 무대 DOM 제거) 스트림도 끊는다 — 안 그러면 백그라운드에서 세그먼트를 계속 받음
  _dhPlayerWatch = setInterval(() => { if (!document.body.contains(host)) dhDestroyPlayer(); }, 2000);
  const cnt = document.getElementById('dh-lvcnt');
  _dhPlayer = window.__snukPlayer(host, channelId, {
    onState: (st, info) => {
      // 재생 중엔 설명 오버레이를 숨기고(hover 시 표시) — 데모 bindStageOver 와 동일
      stage.classList.toggle('playing', st === 'playing');
      if (info && typeof info.viewers === 'number' && cnt) {
        cnt.textContent = `${info.viewers.toLocaleString()}명 시청`;
        cnt.style.display = info.live ? '' : 'none';
      }
    },
    onFallback: () => dhFallbackCrop(stage, channelId, name),
  });
}
// 재생주소가 없는 방송(19금/비공개) — 예전 방식: 치지직 페이지(1620 로드)의 영상 영역(x240,y60,1026 폭)만 크롭.
// 이 폴백만 pointer-events:none(휠 드리프트 방지) — 조작은 "방송 보러가기".
function dhFallbackCrop(stage, channelId, name) {
  const host = document.getElementById('dh-player');
  if (!host) return;
  stage.classList.remove('playing');
  host.className = 'lvplayer';
  host.innerHTML = `<iframe id="dh-stage-frame" title="${esc(name)} 라이브" src="https://chzzk.naver.com/live/${esc(channelId)}"
      scrolling="no" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen
      style="width:1620px;height:900px;pointer-events:none;"></iframe>`;
  dhFitStage();
  if (!window.__dhCropBound) {
    window.__dhCropBound = true;
    window.addEventListener('resize', dhFitStage);
  }
}
function dhFitStage() {
  const stage = document.getElementById('dh-stage');
  const fr = document.getElementById('dh-stage-frame');
  if (!stage || !fr || !stage.clientWidth) return;
  const s = stage.clientWidth / 1026;
  fr.style.transform = `scale(${s}) translate(-240px, -60px)`;
}
function dhPickLive(i) {
  _dhLiveOn = i;
  dhStage();
  const wrap = document.getElementById('dh-livewrap');
  if (wrap && window.innerWidth <= 1180) wrap.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ── 스트리머 스토리 줄
function dhStreamers() {
  const row = document.getElementById('dh-strrow');
  if (!row) return;
  const list = D().streamers || [];
  const sec = document.getElementById('dh-strsec');
  if (!list.length) { if (sec) sec.style.display = 'none'; return; }
  if (sec) sec.style.display = '';
  const live = dhLiveList();
  row.innerHTML = list.map((s) => {
    const idx = live.findIndex((x) => x.name === s.name);
    const onclick = idx >= 0 ? `dhPickLive(${idx})` : `window.__snukNav('/streamers/${s.id}')`;
    return `<button class="strcard" onclick="${onclick}">
      <span class="stravwrap${s.live ? ' live' : ''}">${dhAva(s.name, s.img)}
        ${s.live ? '<span class="strlive">LIVE</span>' : ''}</span>
      <span class="strn">${esc(s.name)}</span>
      <span class="strf">${s.followers != null ? `팔로워 ${s.followers.toLocaleString('ko-KR')}` : '스눅 스트리머'}</span>
    </button>`;
  }).join('');
}

// ── 모집 중인 컨텐츠 — 데모 homeJobCard 그대로
// 데모 ddayTag: 0~14일 남았을 때만, 7일 이하면 urgent
function dhDdayTag(n) {
  if (n == null || n < 0 || n > 14) return '';
  return `<span class="dwrap"><span class="dday${n <= 7 ? ' urgent' : ''}">D-${n}</span></span>`;
}
const dhFmtDate = (s) => (s ? String(s).slice(0, 10).replace(/-/g, '.') : '');
// 데모 contSpecShort — 실제로 있는 값만 채운다(없는 항목을 임의로 "무료/제한 없음"으로 지어내지 않음)
function dhJobSpec(d) {
  const apply = d.applyStart
    ? dhFmtDate(d.applyStart) + (d.applyEnd ? ` ~ ${dhFmtDate(d.applyEnd)}` : '')
    : '상시 모집';
  const sched = d.eventDate ? dhFmtDate(d.eventDate) : '비정기';
  const rows = [
    `<dt>신청 기간</dt><dd>${esc(apply)}</dd>`,
    `<dt>진행 일시</dt><dd>${esc(sched)}</dd>`,
    d.max > 0 ? `<dt>모집 인원</dt><dd>${d.filled}/${d.max}명</dd>` : '',
  ].join('');
  return `<dl class="spec">${rows}</dl>`;
}
function dhJobCard(d, i) {
  const click = cardClickOf(d);
  const prep = isPreparing(d);
  const dd = prep ? `<span class="dwrap"><span class="dday">준비중</span></span>` : dhDdayTag(dhDday(d.applyEnd));
  const byline = d.adminMade === false
    ? `${dhAva('스')}<span style="min-width:0"><span class="dh-row" style="gap:5px">
        <span style="font-weight:700;font-size:13px">스트리머 컨텐츠</span></span>
        <span class="micro dim">스트리머 모집</span></span>`
    : `<span class="ava" style="background:var(--accent);color:#fff">S</span>
       <span style="min-width:0"><span class="dh-row" style="gap:5px">
         <span style="font-weight:700;font-size:13px">SNUK</span><span class="tag t-pri">공식</span></span>
       <span class="micro dim">지원 ${d.filled || 0}명</span></span>`;
  return `<div class="card ov contcard clickcard" onclick="${click}">
    <span class="thumb" style="background:${bgOf(i)};">
      ${d.img ? `<img src="${esc(d.img)}" alt="" onerror="this.remove()">` : ''}${dd}
    </span>
    <div class="cbody">
      <div class="dh-row" style="gap:6px;margin-bottom:7px"><span class="tag t-neu">${d.kind === 'tournament' ? '대회' : '컨텐츠'}</span>${prep ? `<span class="tag t-prep">준비중</span>` : ''}</div>
      <p style="font-size:15px;font-weight:700;letter-spacing:-.02em;margin-bottom:6px">${esc(d.title)}</p>
      <p class="cintro">${esc(d.desc)}</p>
      ${prep ? `<p class="micro dim" style="margin:4px 0 8px">모집이 시작되면 신청할 수 있어요</p>` : `<div class="specfull">${dhJobSpec(d)}</div>`}
      <div class="byline">${byline}</div>
      <button class="btn sm wide" style="margin-top:10px" onclick="event.stopPropagation();${click}">${prep ? '내용 보기' : '자세히 보기'}</button>
    </div>
  </div>`;
}
function dhJobs() {
  const row = document.getElementById('dh-jobrow');
  if (!row) return;
  // 데모와 동일하게 마감 안 된 것만 3장
  const all = [...(D().snukContents || []), ...(D().mugContents || [])]
    .filter((d) => d.status !== 'closed')
    .sort((a, b) => (a.status === 'open' ? 0 : 1) - (b.status === 'open' ? 0 : 1));
  const featured = D().snukFeatured || D().mugFeatured;
  const list = featured
    ? [featured, ...all.filter((x) => !(x.kind === featured.kind && x.id === featured.id))]
    : all;
  row.innerHTML = list.length
    ? list.slice(0, 3).map(dhJobCard).join('')
    : dhEmpty('진행 중인 컨텐츠가 없습니다. 곧 새로운 컨텐츠로 찾아올게요!');
}

// ── 모집 중인 체험단
// 데모 homeTrialCard 그대로
function dhTrialCard(g, i) {
  const left = Math.max(0, (g.max || 0) - (g.members || 0));
  const prep = !!g.preparing;
  const n = prep ? null : dhDday(g.applyEnd);
  const click = prep && g.campaignId ? `openPreview('campaign',${g.campaignId})`
    : g.campaignId && g.applyOpen ? `openApply('campaign',${g.campaignId})` : `window.__snukNav('/games')`;
  return `<div class="card ov contcard clickcard" onclick="${click}">
    <span class="thumb" style="background:${bgOf(i + 3)};">
      ${g.img ? `<img src="${esc(g.img)}" alt="" onerror="this.remove()">` : ''}${prep ? `<span class="dwrap"><span class="dday">준비중</span></span>` : dhDdayTag(n)}
    </span>
    <div class="cbody">
      <div class="dh-row" style="gap:7px;margin-bottom:8px">
        <span class="tag t-neu">게임</span>
        ${prep ? `<span class="tag t-prep">준비중</span>` : `<span class="tag ${g.pick === '선착순' ? 't-warn' : 't-pri'}">${esc(g.pick || '선정')}</span>`}
      </div>
      <p style="font-size:15px;font-weight:700;letter-spacing:-.02em;margin-bottom:10px">${esc(g.name)}</p>
      ${prep ? `<p class="micro dim" style="margin:0 0 6px">모집이 시작되면 신청할 수 있어요</p>` : `<div class="qtybar">
        <span class="qb"><span class="ql">지원 수량</span><span class="qv">${g.max || 0}</span></span>
        <span class="qb"><span class="ql">남은 수량</span><span class="qv"${left ? ' style="color:var(--green)"' : ''}>${left}</span></span>
        <span class="qb"><span class="ql">지원 수</span><span class="qv">${g.members || 0}</span></span>
        <span class="qb"><span class="ql">모집 마감</span><span class="qv" style="font-size:13px">${n == null ? '—' : n < 0 ? '마감' : `D-${n}`}</span></span>
      </div>`}
      ${!prep && g.applyEnd ? `<p class="micro dim" style="margin-top:6px">마감일 ${esc(dhFmtDate(g.applyEnd))}</p>` : ''}
      <div class="byline"><span class="plogo">${esc((g.publisher || 'S').slice(0, 1))}</span>
        <span><span style="display:block;font-weight:700;font-size:13px">${esc(g.publisher || 'SNUK')}</span>
        <span class="micro dim">협력사</span></span></div>
      ${g.applyOpen && g.campaignId
        ? `<button class="btn pri sm wide" style="margin-top:10px" onclick="event.stopPropagation();openApply('campaign',${g.campaignId})">신청하기</button>`
        : `<button class="btn sm wide" style="margin-top:10px" onclick="event.stopPropagation();${click}">${prep ? '내용 보기' : '자세히 보기'}</button>`}
    </div>
  </div>`;
}
function dhTrials() {
  const row = document.getElementById('dh-trialrow');
  if (!row) return;
  const games = [...(D().games || [])].sort((a, b) => (a.applyOpen ? 0 : 1) - (b.applyOpen ? 0 : 1));
  row.innerHTML = games.length
    ? games.slice(0, 4).map(dhTrialCard).join('')
    : dhEmpty('모집 중인 체험단이 없습니다.');
}

// ── 체험단 후기
function dhReviews() {
  const row = document.getElementById('dh-revrow');
  if (!row) return;
  const list = D().trialReviews || [];
  if (!list.length) { row.innerHTML = dhEmpty('아직 등록된 체험단 후기가 없습니다.'); return; }
  row.innerHTML = list.slice(0, 3).map((r, i) => {
    const yt = r.videoUrl ? (r.videoUrl.match(/(?:youtu\.be\/|[?&]v=|\/embed\/|\/shorts\/)([\w-]{11})/) || [])[1] : null;
    const vid = yt
      ? `<div class="revvid"><iframe loading="lazy" src="https://www.youtube-nocookie.com/embed/${esc(yt)}" allowfullscreen></iframe></div>`
      : r.videoUrl
        ? `<div class="revvid"><a class="nolink" href="${esc(r.videoUrl)}" target="_blank" rel="noopener"
             onclick="event.stopPropagation()"><span class="pl">▶</span><b>후기 영상 보기</b></a></div>`
        : '';
    const go = r.campaignId ? `window.__snukNav('/campaigns/${r.campaignId}/reviews')` : `window.__snukNav('/games')`;
    return `<div class="revcard">
      <div class="revtop" style="background:${bgOf(i + 1)};">
        ${r.img ? `<img class="bg" src="${esc(r.img)}" alt="" onerror="this.remove()">` : ''}
        ${vid}
      </div>
      <div class="revname"><b>${esc(r.campaignTitle)}</b>
        ${r.publisher ? `<span class="tag t-neu">${esc(r.publisher)}</span>` : ''}</div>
      <div class="revbody">
        <div class="dh-row"><span class="tag t-pri">체험단 후기</span><span class="micro dim">${esc(r.date)}</span></div>
        <p class="ro" style="margin-top:8px">${esc(r.title)}</p>
        <p class="rb">${esc(r.content)}</p>
        <div class="dh-row" style="margin-top:auto">
          <button class="btn sm" onclick="${go}">후기 전체 보기</button>
        </div>
      </div>
      <div class="revadmin"><div class="dh-row">${dhAva(r.author)}
        <span><span style="display:block;font-size:13px;font-weight:700">${esc(r.author)}</span>
        <span class="micro dim">후기 작성자</span></span></div></div>
    </div>`;
  }).join('');
}

// ── 커뮤니티 인기글 / 스눅 뉴스
function dhLists() {
  const com = document.getElementById('dh-community');
  if (com) {
    const posts = D().communityPosts || [];
    com.innerHTML = posts.length
      ? posts.map((p, i) => `<button class="bestrow" onclick="window.__snukNav('/community/${p.id}')">
          <span class="brk${i < 3 ? ' hot' : ''}">${i + 1}</span>
          <span class="bt">${esc(p.title)}</span>
          <span class="cmt">${esc(p.boardName)}</span>
        </button>`).join('')
      : dhEmpty('아직 등록된 게시글이 없습니다.');
  }
  const nw = document.getElementById('dh-newslist');
  if (nw) {
    const news = D().news || [];
    nw.innerHTML = news.length
      ? news.slice(0, 5).map((n) => `<button class="bestrow" onclick="window.__snukNav('/news/${n.id}')">
          <span class="tag t-neu">뉴스</span>
          <span class="bt">${esc(n.title)}</span>
          <span class="cmt">${esc(n.date)}</span>
        </button>`).join('')
      : dhEmpty('아직 등록된 기사가 없습니다.');
  }
}

// ── 방송도우미 인기 소스
function dhTools() {
  const row = document.getElementById('dh-toolrow');
  if (!row) return;
  const list = D().resources || [];
  if (!list.length) { row.innerHTML = dhEmpty('등록된 소스가 없습니다.'); return; }
  row.innerHTML = list.slice(0, 12).map((t, i) => {
    const click = t.url ? `window.open('${esc(t.url)}','_blank')` : `window.__snukNav('/resources')`;
    return `<button class="minicard" onclick="${click}">
      <span class="minithumb sq" style="background:${bgOf(i)};">
        ${t.img ? `<img src="${esc(t.img)}" alt="" onerror="this.remove()">` : ''}
        <span class="minitag">무료소스</span>
      </span>
      <span class="minit">${esc(t.title)}</span>
      <span class="micro dim">${esc(t.date)}</span>
    </button>`;
  }).join('');
}

function initDemoHome() {
  if (!document.getElementById('home-demo')) return;
  dhBoard();
  dhStage();
  dhStreamers();
  dhJobs();
  dhTrials();
  dhReviews();
  dhLists();
  dhTools();
}

// ════════════════════════════════════════════
// INIT (페이지 마운트마다 SnukSections/SnukShell 이 호출 — 멱등)
// ════════════════════════════════════════════
function __snukInit() {
  bindOverlayClose();
  initDemoHome();
  renderContentSliders();
  renderContentsPage();
  renderFeatured('mug-featured', D().mugFeatured, 'SIGNATURE CONTENT');
  initGameTrial();
  initGameVideos();
  initVideos();
  if (window._rerenderGoods) window._rerenderGoods();
  initPartners();
  initStreamerChannels();
  initStreamerPost();
  renderNotices();
  applySiteImages();
  setActiveNav(location.pathname);
}

// 사이드바 메뉴 (어드민 설정 — 항목 8 확장):
//  MENU_{KEY}='0' 숨김 · MENU_LABEL_{KEY} 메뉴명 변경 · MENU_CUSTOM=JSON 커스텀 메뉴 추가
function applyMenuVisibility(ss) {
  document.querySelectorAll('.rs-item[data-menu]').forEach((btn) => {
    const v = ss[`MENU_${btn.dataset.menu}`];
    btn.style.display = v === '0' ? 'none' : '';
    const label = ss[`MENU_LABEL_${btn.dataset.menu}`];
    if (label && label !== '-') btn.textContent = label;
  });
  // 커스텀 메뉴 — 관리자 항목 바로 앞에 삽입 (데스크톱 사이드바 + 모바일 드로어 둘 다)
  let custom = [];
  try { custom = JSON.parse(ss.MENU_CUSTOM || '[]'); } catch (e) { custom = []; }
  if (!Array.isArray(custom)) custom = [];
  document.querySelectorAll('.rs-item[data-custom]').forEach((el) => el.remove());
  document.querySelectorAll('.rs-item.rs-admin-item').forEach((adminBtn) => {
    custom.forEach((m) => {
      if (!m || !m.label) return;
      const b = document.createElement('button');
      b.className = 'rs-item';
      b.dataset.custom = '1';
      b.textContent = m.label;
      b.onclick = () => {
        const url = String(m.url || '/');
        if (/^https?:\/\//i.test(url)) window.open(url, '_blank', 'noopener');
        else if (/^\/crew\//.test(url)) window.location.assign(url); // 크루 페이지 = SPA 밖 정적 HTML → 전체 이동
        else if (window.__snukNav) window.__snukNav(url);
      };
      adminBtn.parentElement.insertBefore(b, adminBtn);
    });
  });
}

// 어드민 "설정" 탭에서 바꾼 히어로/배너 이미지·문구 적용 ('-'=미설정 → 마크업 기본값 유지, 'none'=이미지 제거)
function applySiteImages() {
  const ss = (D() && D().siteSettings) || {};
  const set = (v) => v && v !== '-';
  const applyImg = (sel, url) => {
    if (!set(url)) return;
    const el = document.querySelector(sel);
    if (!el) return;
    if (url === 'none') { el.style.display = 'none'; return; } // 이미지 없애기(항목 16)
    el.style.display = '';
    if (el.getAttribute('src') !== url) el.setAttribute('src', url);
  };
  applyMenuVisibility(ss);
  const applyText = (sel, text) => {
    const el = document.querySelector(sel);
    if (!el) return;
    if (text === undefined || text === null || text === '-') return; // 미설정 → 기본 문구 유지
    if (String(text).trim() === '') { el.style.display = 'none'; return; } // 빈값 저장 → 문구 숨김(항목 4)
    el.style.display = '';
    el.textContent = text;
  };
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
