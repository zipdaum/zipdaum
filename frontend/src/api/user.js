import client from "./client";

export async function getUserInfo() {
  const response = await client.get("/users/info");
  return response.data;
}

export async function deleteUserInfo(payload) {
  const response = await client.delete("/users/info", {
    data: payload
  });
  return response.data;
}
