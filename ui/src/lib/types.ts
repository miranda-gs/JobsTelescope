export interface SearchCommand {
  command: 'search';
  query: string;
  region: 'BRAZIL' | 'INTERNATIONAL';
  platforms?: string[];
  maxResults?: number;
}

export interface ProgressEvent {
  type: 'progress';
  platform: string;
  percentage: number;
}

export interface CompletedEvent {
  type: 'completed';
  jobsFound: number;
  output: string;
}

export interface ErrorEvent {
  type: 'error';
  message: string;
}

export type CoreEvent = ProgressEvent | CompletedEvent | ErrorEvent;

export interface Job {
  title: { value: string };
  company: string;
  location: { value: string };
  url: { value: string };
  platform: string;
  region: string;
  source: string;
  foundAt: string;
}
