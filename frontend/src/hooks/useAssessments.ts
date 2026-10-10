import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, listOf } from '../api/client'
import type { AssessmentRequest, AssessmentResponse } from '../api/types'

export function useAssessment(id: string | undefined) {
  return useQuery({ queryKey: ['assessments', id], queryFn: () => api<AssessmentResponse>(`/assessments/${id}`), enabled: !!id })
}

export function useAthleteAssessments(athleteId: string | undefined) {
  return useQuery({
    queryKey: ['athletes', athleteId, 'assessments'],
    queryFn: () => listOf<AssessmentResponse>(`/athletes/${athleteId}/assessments`),
    enabled: !!athleteId,
  })
}

/**
 * The backend ranks ASSESSMENTS, so an athlete assessed 3 times appears 3 times.
 * Results come sorted best-first, so keeping the first entry per athleteId
 * gives each athlete's best assessment.
 */
export function bestPerAthlete(list: AssessmentResponse[]): AssessmentResponse[] {
  const seen = new Set<string>()
  return list.filter((a) => {
    if (seen.has(a.athleteId)) return false
    seen.add(a.athleteId)
    return true
  })
}

export function useRanking(enabled = true) {
  return useQuery({
    queryKey: ['ranking'],
    queryFn: async () => bestPerAthlete(await listOf<AssessmentResponse>('/assessments/ranking')),
    enabled,
  })
}

function useInvalidateAssessmentData() {
  const qc = useQueryClient()
  return () => {
    qc.invalidateQueries({ queryKey: ['assessments'] })
    qc.invalidateQueries({ queryKey: ['ranking'] })
    qc.invalidateQueries({ queryKey: ['athletes'] })
  }
}

export function useSaveAssessment() {
  const invalidate = useInvalidateAssessmentData()
  return useMutation({
    mutationFn: ({ id, data }: { id?: string; data: AssessmentRequest }) =>
      id
        ? api<AssessmentResponse>(`/assessments/${id}`, { method: 'PUT', body: data })
        : api<AssessmentResponse>('/assessments', { method: 'POST', body: data }),
    onSuccess: invalidate,
  })
}

export function useDeleteAssessment() {
  const invalidate = useInvalidateAssessmentData()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/assessments/${id}`, { method: 'DELETE' }),
    onSuccess: invalidate,
  })
}
