import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, listOf, orNull } from '../api/client'
import type { Athlete, AthleteRequest, PhysicalProfile, PhysicalProfileRequest } from '../api/types'

export function useAthletes(enabled = true) {
  return useQuery({ queryKey: ['athletes'], queryFn: () => listOf<Athlete>('/athletes'), enabled })
}

export function useMyAthlete(enabled = true) {
  return useQuery({
    queryKey: ['athletes', 'me'],
    queryFn: () => orNull(api<Athlete>('/athletes/me')),
    enabled,
  })
}

export function useUpdateMyContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (contactNumber: string) => api<Athlete>('/athletes/me/contact', { method: 'PATCH', body: { contactNumber } }),
    onSuccess: (saved) => {
      qc.setQueryData(['athletes', saved.id], saved)
      qc.invalidateQueries({ queryKey: ['athletes', 'me'] })
    },
  })
}

export function useLinkAccount(athleteId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (userId: string | null) => api<Athlete>(`/athletes/${athleteId}/account`, { method: 'PUT', body: { userId } }),
    onSuccess: (saved) => qc.setQueryData(['athletes', athleteId], saved),
  })
}

export function useAthlete(id: string | undefined) {
  return useQuery({ queryKey: ['athletes', id], queryFn: () => api<Athlete>(`/athletes/${id}`), enabled: !!id })
}

export function useSaveAthlete() {
  const qc = useQueryClient()
  return useMutation({
    // Create: POST /sports/{sportId}/athletes?teamId=...   Update: PUT /athletes/{id}?teamId=...
    // Omitting teamId on update removes the athlete from their team (backend rule).
    mutationFn: ({ id, sportId, teamId, data }: { id?: string; sportId: string; teamId?: string; data: AthleteRequest }) =>
      id
        ? api<Athlete>(`/athletes/${id}`, { method: 'PUT', body: data, params: { teamId } })
        : api<Athlete>(`/sports/${sportId}/athletes`, { method: 'POST', body: data, params: { teamId } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['athletes'] }),
  })
}

export function useDeactivateAthlete() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<Athlete>(`/athletes/${id}/deactivate`, { method: 'PATCH' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['athletes'] }),
  })
}

export function useDeleteAthlete() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/athletes/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['athletes'] }),
  })
}

/** 404 means "no profile recorded yet", so it resolves to null instead of an error. */
export function usePhysicalProfile(athleteId: string | undefined) {
  return useQuery({
    queryKey: ['athletes', athleteId, 'physical'],
    queryFn: () => orNull(api<PhysicalProfile>(`/athletes/${athleteId}/physical-profile`)),
    enabled: !!athleteId,
  })
}

export function useSavePhysicalProfile(athleteId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: PhysicalProfileRequest) =>
      api<PhysicalProfile>(`/athletes/${athleteId}/physical-profile`, { method: 'PUT', body: data }),
    onSuccess: (saved) => qc.setQueryData(['athletes', athleteId, 'physical'], saved),
  })
}
