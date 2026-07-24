import { z } from 'zod';

const regionSchema = z.enum(['BRAZIL', 'INTERNATIONAL']);
const platformSchema = z.enum([
  'GUPY',
  'CATHO',
  'VAGAS',
  'GLASSDOOR',
  'TRAMPOS',
  'PROGRAMATHOR',
  'REMOTAR',
  'NINETY_NINE_JOBS',
  'EMPREGOS',
  'INDEED_BR',
  'INFOJOBS',
  'WELLFOUND',
  'WE_WORK_REMOTELY',
  'FLEXJOBS',
  'DICE',
  'LEVELS_FYI',
  'HIRED',
  'OTTA',
  'REMOTE_OK',
  'JOBICY',
  'NERDIN',
  'PICPAY',
  'IFOOD',
  'MERCADO_LIVRE',
  'REMOTIVE',
]);

export const searchInputSchema = z.object({
  query: z.string().min(1, 'Query is required'),
  region: regionSchema,
  platforms: z.array(platformSchema).optional(),
  maxResults: z.number().int().positive().optional(),
});

export type SearchInput = z.infer<typeof searchInputSchema>;

export function validateSearchInput(input: unknown): SearchInput {
  return searchInputSchema.parse(input);
}
