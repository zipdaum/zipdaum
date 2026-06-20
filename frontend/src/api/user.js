import client from "./client";

export async function getUserInfo() {
  const response = await client.get("/users/info");
  return response.data;
}
