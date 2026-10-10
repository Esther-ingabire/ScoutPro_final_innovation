import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, listOf, orNull } from '../api/client'
import type { Role, Scout, ScoutRequest, User } from '../api/types'

export function useUsers(enabled = true) {
  return useQuery({ queryKey: ['users'], queryFn: () => listOf<User>('/users'), enabled })
}

export function useUpdateRoles() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, roles }: { userId: string; roles: Role[] }) =>
      api<User>(`/users/${userId}/roles`, { method: 'PUT', body: { roles } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
  })
}

export function useScouts(enabled = true) {
  return useQuery({ queryKey: ['scouts'], queryFn: () => listOf<Scout>('/scouts'), enabled })
}

/** The signed-in scout's own profile. null = this SCOUT user has no profile yet (404). */
export function useMyScout(enabled = true) {
  return useQuery({ queryKey: ['scouts', 'me'], queryFn: () => orNull(api<Scout>('/scouts/me')), enabled })
}

export function useCreateScout() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: ScoutRequest }) =>
      api<Scout>(`/users/${userId}/scout`, { method: 'POST', body: data }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['scouts'] }),
  })
}

export function useDeactivateScout() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<Scout>(`/scouts/${id}/deactivate`, { method: 'PATCH' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['scouts'] }),
  })
}
