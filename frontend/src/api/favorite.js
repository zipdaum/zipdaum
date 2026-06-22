import client from "./client";

export async function getFavoriteRegions() {
  const response = await client.get("/users/info/regions");
  return response.data;
}

export async function getFavoriteRegionCandidates(keyword) {
  const response = await client.get("/users/info/regions/candidates", {
    params: {
      keyword,
    },
  });
  return response.data;
}

export async function addFavoriteRegion({ sggCd, umdNm }) {
  const response = await client.post("/users/info/regions", {
    sggCd,
    umdNm,
  });
  return response.data;
}

export async function deleteFavoriteRegion({ sggCd, umdNm }) {
  const response = await client.delete("/users/info/regions", {
    params: {
      sggCd,
      umdNm,
    },
  });
  return response.data;
}

export async function getFavoriteProperties() {
  const response = await client.get("/users/info/properties");
  return response.data;
}

export async function addFavoriteProperty(propertyId) {
  const response = await client.post("/users/info/properties", {
    propertyId,
  });
  return response.data;
}

export async function deleteFavoriteProperty(propertyId) {
  const response = await client.delete("/users/info/properties", {
    params: {
      propertyId,
    },
  });
  return response.data;
}
