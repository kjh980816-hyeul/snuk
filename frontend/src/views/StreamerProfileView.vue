<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { streamerApi } from '@/api'
import type { StreamerPost, StreamerProfile } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

// 스트리머 프로필: 팔로우 + 개인 게시판 (작성=로그인 회원, 삭제=작성자·해당 스트리머·ADMIN)
const route = useRoute()
const auth = useAuthStore()

const profile = ref<StreamerProfile | null>(null)
const posts = ref<StreamerPost[]>([])
const loading = ref(true)
const notFound = ref(false)

const writeOpen = ref(false)
const title = ref('')
const content = ref('')
const submitting = ref(false)
const followBusy = ref(false)

const streamerId = computed(() => Number(route.params.id))
const isMe = computed(() => auth.me?.id === profile.value?.streamer.id)

const platLabel: Record<string, string> = { CHZZK: '치지직', SOOP: '숲', CIME: '씨미' }
const platColor: Record<string, string> = { CHZZK: '#00c73c', SOOP: '#34c7ff', CIME: '#7c5cff' }

async function load() {
  loading.value = true
  notFound.value = false
  try {
    profile.value = await streamerApi.profile(streamerId.value)
    posts.value = await streamerApi.posts(streamerId.value)
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!profile.value || followBusy.value) return
  if (!auth.isLoggedIn) {
    ;(window as unknown as { openLogin?: () => void }).openLogin?.()
    return
  }
  followBusy.value = true
  try {
    const res = profile.value.following
      ? await streamerApi.unfollow(streamerId.value)
      : await streamerApi.follow(streamerId.value)
    profile.value = { ...profile.value, following: res.following, followCount: res.followCount }
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '요청에 실패했습니다.')
  } finally {
    followBusy.value = false
  }
}

async function submitPost() {
  if (!title.value.trim() || submitting.value) return
  submitting.value = true
  try {
    await streamerApi.writePost(streamerId.value, {
      title: title.value.trim(),
      content: content.value.trim(),
    })
    title.value = ''
    content.value = ''
    writeOpen.value = false
    posts.value = await streamerApi.posts(streamerId.value)
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '글 작성에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

async function removePost(p: StreamerPost) {
  if (!confirm(`'${p.title}' 글을 삭제할까요?`)) return
  try {
    await streamerApi.deletePost(p.id)
    posts.value = await streamerApi.posts(streamerId.value)
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '삭제 권한이 없습니다.')
  }
}

function dt(v: string) {
  return v?.slice(0, 16).replace('T', ' ')
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <section class="sp-page">
    <div class="inner">
      <RouterLink to="/streamers" class="sp-back">&lt; 스트리머 목록</RouterLink>

      <div v-if="loading" class="sp-empty">불러오는 중…</div>
      <div v-else-if="notFound || !profile" class="sp-empty">존재하지 않는 스트리머입니다.</div>

      <template v-else>
        <!-- 프로필 헤더 -->
        <div class="sp-card">
          <div class="sp-avatar">
            <img v-if="profile.streamer.profileImageUrl" :src="profile.streamer.profileImageUrl" alt="" />
            <span v-else>{{ profile.streamer.nickname.slice(0, 1) }}</span>
          </div>
          <div class="sp-info">
            <div class="sp-name-row">
              <span class="sp-name">{{ profile.streamer.nickname }}</span>
              <span class="sp-plat" :style="{ color: platColor[profile.streamer.provider], borderColor: platColor[profile.streamer.provider] + '55', background: platColor[profile.streamer.provider] + '14' }">
                {{ platLabel[profile.streamer.provider] ?? profile.streamer.provider }}
              </span>
            </div>
            <div class="sp-stats-row">
              <span>SNUK 팔로워 <b>{{ profile.followCount.toLocaleString('ko-KR') }}</b></span>
              <span v-if="profile.streamer.followerCount != null">
                플랫폼 팔로워 <b>{{ profile.streamer.followerCount.toLocaleString('ko-KR') }}</b>
              </span>
            </div>
            <div class="sp-actions">
              <button v-if="!isMe" class="sp-btn" :class="{ on: profile.following }" :disabled="followBusy" @click="toggleFollow">
                {{ profile.following ? '✓ 팔로잉' : '+ 팔로우' }}
              </button>
              <a v-if="profile.streamer.channelUrl" :href="profile.streamer.channelUrl" target="_blank" rel="noopener">
                <button class="sp-btn ghost">방송국 가기 ↗</button>
              </a>
            </div>
          </div>
        </div>

        <!-- 개인 게시판 -->
        <div class="sp-board-head">
          <h3>{{ profile.streamer.nickname }}의 게시판 <span class="sp-count">{{ posts.length }}</span></h3>
          <button v-if="auth.isLoggedIn" class="sp-btn" @click="writeOpen = !writeOpen">
            {{ writeOpen ? '작성 취소' : '글쓰기' }}
          </button>
          <button v-else class="sp-btn ghost" @click="auth.login()">로그인 후 글쓰기</button>
        </div>

        <div v-if="writeOpen" class="sp-write">
          <input v-model="title" placeholder="제목" maxlength="200" />
          <textarea v-model="content" rows="5" placeholder="스트리머에게 남길 이야기를 적어보세요"></textarea>
          <button class="sp-btn" :disabled="submitting || !title.trim()" @click="submitPost">
            {{ submitting ? '등록 중…' : '등록' }}
          </button>
        </div>

        <div v-if="!posts.length" class="sp-empty">아직 글이 없습니다. 첫 글을 남겨보세요!</div>
        <article v-for="p in posts" :key="p.id" class="sp-post">
          <div class="sp-post-head">
            <div class="sp-post-author">
              <div class="sp-post-avatar">
                <img v-if="p.authorImageUrl" :src="p.authorImageUrl" alt="" />
                <span v-else>{{ p.authorName.slice(0, 1) }}</span>
              </div>
              <div>
                <div class="sp-post-title">{{ p.title }}</div>
                <div class="sp-post-meta">{{ p.authorName }} · {{ dt(p.createdAt) }}</div>
              </div>
            </div>
            <button v-if="p.deletable" class="sp-del" @click="removePost(p)">삭제</button>
          </div>
          <p v-if="p.content" class="sp-post-content">{{ p.content }}</p>
        </article>
      </template>
    </div>
  </section>
</template>

<style scoped>
/* 시안 다크 테마 — home-snuk.css 변수 사용 */
.sp-back { font-size: 13px; color: var(--text3); display: inline-block; margin-bottom: 14px; text-decoration: none; }

.sp-card {
  display: flex; align-items: center; gap: 22px; flex-wrap: wrap;
  background: var(--bg2); border: 1px solid var(--border); border-radius: 16px;
  padding: 26px 28px; margin-bottom: 28px;
}
.sp-avatar {
  width: 96px; height: 96px; border-radius: 50%; overflow: hidden; flex: none;
  background: linear-gradient(135deg, var(--accent), var(--accent2));
  display: flex; align-items: center; justify-content: center;
  font-size: 34px; font-weight: 700; color: #fff; border: 2px solid var(--border2);
}
.sp-avatar img { width: 100%; height: 100%; object-fit: cover; }
.sp-name-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.sp-name { font-size: 22px; font-weight: 800; color: var(--text); }
.sp-plat { font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 20px; border: 1px solid; }
.sp-stats-row { display: flex; gap: 16px; font-size: 13px; color: var(--text3); margin-top: 8px; flex-wrap: wrap; }
.sp-stats-row b { color: var(--text); }
.sp-actions { display: flex; gap: 8px; margin-top: 14px; flex-wrap: wrap; }
.sp-btn {
  padding: 9px 18px; font-size: 13px; font-weight: 700; border-radius: 10px; border: none; cursor: pointer;
  background: linear-gradient(135deg, var(--accent), var(--accent2)); color: #111;
}
.sp-btn.on { background: var(--bg3); color: var(--text2); border: 1px solid var(--border2); }
.sp-btn.ghost { background: transparent; color: var(--text2); border: 1px solid var(--border); }
.sp-btn:disabled { opacity: .5; cursor: not-allowed; }

.sp-board-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 14px; }
.sp-board-head h3 { font-size: 17px; color: var(--text); margin: 0; }
.sp-count { font-size: 12px; color: var(--text3); font-weight: 600; margin-left: 4px; }

.sp-write {
  background: var(--bg2); border: 1px solid var(--border); border-radius: 12px;
  padding: 16px; margin-bottom: 16px; display: flex; flex-direction: column; gap: 10px;
}
.sp-write input, .sp-write textarea {
  width: 100%; padding: 10px 12px; border: 1px solid var(--border); background: var(--bg3);
  color: var(--text); border-radius: 8px; font-size: 14px; outline: none;
}
.sp-write button { align-self: flex-end; }

.sp-post { background: var(--bg2); border: 1px solid var(--border); border-radius: 12px; padding: 16px 18px; margin-bottom: 10px; }
.sp-post-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.sp-post-author { display: flex; align-items: center; gap: 10px; min-width: 0; }
.sp-post-avatar {
  width: 36px; height: 36px; border-radius: 50%; overflow: hidden; flex: none;
  background: var(--bg3); display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; color: var(--text2);
}
.sp-post-avatar img { width: 100%; height: 100%; object-fit: cover; }
.sp-post-title { font-size: 14px; font-weight: 700; color: var(--text); }
.sp-post-meta { font-size: 11px; color: var(--text3); margin-top: 2px; }
.sp-del {
  flex: none; font-size: 11px; font-weight: 700; padding: 5px 11px; border-radius: 8px; cursor: pointer;
  background: rgba(239, 68, 68, .1); color: #ef4444; border: 1px solid rgba(239, 68, 68, .3);
}
.sp-post-content { margin: 10px 0 0 46px; font-size: 13px; color: var(--text2); white-space: pre-wrap; line-height: 1.7; }

.sp-empty {
  border: 1px dashed var(--border2); border-radius: 12px; padding: 40px 16px;
  text-align: center; color: var(--text3); font-size: 13px;
}
</style>
