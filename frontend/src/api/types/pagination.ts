export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface PageParams {
  page?: number
  size?: number
  sort?: string
}

export interface ListParams extends PageParams {
  search?: string
}

/** Common Spring Data-style list query object. */
export function buildListQuery(params?: ListParams): Record<string, unknown> {
  const query: Record<string, unknown> = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
  }
  if (params?.sort != null && params.sort !== '') query.sort = params.sort
  if (params?.search?.trim()) query.search = params.search.trim()
  return query
}
