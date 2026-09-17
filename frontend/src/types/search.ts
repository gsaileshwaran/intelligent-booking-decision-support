export interface SearchCard {
  entityType: string;
  entityId: number;
  title: string;
  subtitle: string;
  badge: string;
  rankPosition: number;
  retrievalMethod: string;
  posterUrl?: string;
}

export interface SearchIndexStatusResponse {
  totalEntitiesIndexed: number;
  lastIndexedAt?: string;
  indexStatus: string;
  embeddingModelStatus?: string;
}
