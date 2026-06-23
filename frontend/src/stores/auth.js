import { computed, ref } from "vue";

const accessToken = ref(localStorage.getItem("accessToken") || "");
const user = ref(readStoredUser());

export const isLoggedIn = computed(() => Boolean(accessToken.value));
export const currentUser = computed(() => user.value);
export const userRole = computed(() => user.value?.role || "");

export function saveAuth(authResponse) {
  const userInfo = {
    userId: authResponse.userId,
    email: authResponse.email,
    name: authResponse.name,
    role: authResponse.role,
  };

  accessToken.value = authResponse.accessToken;
  user.value = userInfo;

  localStorage.setItem("accessToken", authResponse.accessToken);
  localStorage.setItem("user", JSON.stringify(userInfo));
}

export function clearAuth() {
  accessToken.value = "";
  user.value = null;

  localStorage.removeItem("accessToken");
  localStorage.removeItem("user");
}

function readStoredUser() {
  const storedUser = localStorage.getItem("user");

  if (!storedUser) {
    return null;
  }

  try {
    return JSON.parse(storedUser);
  } catch {
    localStorage.removeItem("user");
    return null;
  }
}
