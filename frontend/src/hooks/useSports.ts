import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, listOf } from '../api/client'
import type { Criterion, CriterionRequest, Sport, SportRequest, Team, TeamRequest } from '../api/types'

// Query keys are the cache "addresses". Invalidating ['sports'] also refreshes
// every key that starts with it, e.g. ['sports', id, 'criteria'].

export function useSports() {
  return useQuery({ queryKey: ['sports'], queryFn: () => listOf<Sport>('/sports') })
}

export function useCriteria(sportId: string | undefined) {
  return useQuery({
    queryKey: ['sports', sportId, 'criteria'],
    queryFn: () => listOf<Criterion>(`/sports/${sportId}/criteria`),
    enabled: !!sportId, // do not run until a sport is chosen
  })
}

export function useTeams(sportId: string | undefined) {
  return useQuery({
    queryKey: ['sports', sportId, 'teams'],
    queryFn: () => listOf<Team>(`/sports/${sportId}/teams`),
    enabled: !!sportId,
  })
}

// ---------- admin mutations ----------
export function useSaveSport() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id?: string; data: SportRequest }) =>
      id ? api<Sport>(`/sports/${id}`, { method: 'PUT', body: data }) : api<Sport>('/sports', { method: 'POST', body: data }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sports'] }),
  })
}

export function useDeleteSport() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/sports/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sports'] }),
  })
}

export function useSaveCriterion(sportId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id?: string; data: CriterionRequest }) =>
      id
        ? api<Criterion>(`/criteria/${id}`, { method: 'PUT', body: data })
        : api<Criterion>(`/sports/${sportId}/criteria`, { method: 'POST', body: data }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sports', sportId, 'criteria'] }),
  })
}

export function useDeleteCriterion(sportId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/criteria/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sports', sportId, 'criteria'] }),
  })
}

export function useSaveTeam(sportId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id?: string; data: TeamRequest }) =>
      id ? api<Team>(`/teams/${id}`, { method: 'PUT', body: data }) : api<Team>(`/sports/${sportId}/teams`, { method: 'POST', body: data }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sports', sportId, 'teams'] }),
  })
}

export function useDeleteTeam(sportId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/teams/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sports', sportId, 'teams'] }),
  })
}
