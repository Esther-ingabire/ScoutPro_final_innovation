import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, listOf } from '../api/client'
import type { Shortlist, ShortlistRequest } from '../api/types'

export function useShortlists(enabled = true) {
  return useQuery({ queryKey: ['shortlists'], queryFn: () => listOf<Shortlist>('/shortlists'), enabled })
}

export function useSaveShortlist() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id?: string; data: ShortlistRequest }) =>
      id ? api<Shortlist>(`/shortlists/${id}`, { method: 'PUT', body: data }) : api<Shortlist>('/shortlists', { method: 'POST', body: data }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['shortlists'] }),
  })
}

export function useDeleteShortlist() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/shortlists/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['shortlists'] }),
  })
}

export function useAddToShortlist() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ shortlistId, athleteId }: { shortlistId: string; athleteId: string }) =>
      api<Shortlist>(`/shortlists/${shortlistId}/athletes/${athleteId}`, { method: 'POST' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['shortlists'] }),
  })
}

export function useRemoveFromShortlist() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ shortlistId, athleteId }: { shortlistId: string; athleteId: string }) =>
      api<Shortlist>(`/shortlists/${shortlistId}/athletes/${athleteId}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['shortlists'] }),
  })
}
