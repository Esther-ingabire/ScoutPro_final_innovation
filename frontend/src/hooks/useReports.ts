import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, listOf, orNull } from '../api/client'
import type { ScoutingReport, ScoutingReportRequest } from '../api/types'

export function useReport(assessmentId: string | undefined) {
  return useQuery({
    queryKey: ['assessments', assessmentId, 'report'],
    queryFn: () => orNull(api<ScoutingReport>(`/assessments/${assessmentId}/report`)),
    enabled: !!assessmentId,
  })
}

export function useAthleteReports(athleteId: string | undefined) {
  return useQuery({
    queryKey: ['athletes', athleteId, 'reports'],
    queryFn: () => listOf<ScoutingReport>(`/athletes/${athleteId}/reports`),
    enabled: !!athleteId,
  })
}

export function useSearchReports(q: string) {
  return useQuery({
    queryKey: ['reports', 'search', q],
    queryFn: () => listOf<ScoutingReport>('/reports/search', { q }),
    enabled: q.trim().length > 0,
  })
}

export function useSaveReport(assessmentId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ exists, data }: { exists: boolean; data: ScoutingReportRequest }) =>
      api<ScoutingReport>(`/assessments/${assessmentId}/report`, { method: exists ? 'PUT' : 'POST', body: data }),
    onSuccess: (saved) => {
      qc.setQueryData(['assessments', assessmentId, 'report'], saved)
      qc.invalidateQueries({ queryKey: ['reports'] })
      qc.invalidateQueries({ queryKey: ['athletes', saved.athleteId, 'reports'] })
    },
  })
}

export function useDeleteReport(assessmentId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => api<void>(`/assessments/${assessmentId}/report`, { method: 'DELETE' }),
    onSuccess: () => {
      qc.setQueryData(['assessments', assessmentId, 'report'], null)
      qc.invalidateQueries({ queryKey: ['reports'] })
      qc.invalidateQueries({ queryKey: ['athletes'] })
    },
  })
}
