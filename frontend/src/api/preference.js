import client from "./client";

export async function getUserPreferences() {
  const response = await client.get("/users/info/preferences");
  return response.data;
}

export async function getUserPreferenceRegionCandidates(keyword) {
  const response = await client.get("/users/info/preferences/regions/candidates", {
    params: {
      keyword,
    },
  });
  return response.data;
}

export async function saveUserPreferences(preferences) {
  const response = await client.put("/users/info/preferences", { preferences });
  return response.data;
}
