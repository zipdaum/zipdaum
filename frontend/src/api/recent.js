import client from "./client";

export async function getRecentProperties() {
  const response = await client.get("/users/info/recent-properties");
  return response.data;
}
