import { useCallback, useEffect, useRef, useState } from 'react';
import { CoreClient } from '../lib/core-client.ts';
import type { CoreEvent, SearchCommand } from '../lib/types.ts';

interface CoreClientState {
  status: 'idle' | 'connecting' | 'running' | 'completed' | 'error';
  progress: { platform: string; percentage: number } | null;
  jobsFound: number;
  outputPath: string | null;
  error: string | null;
}

export function useCoreClient(jarPath: string) {
  const [state, setState] = useState<CoreClientState>({
    status: 'idle',
    progress: null,
    jobsFound: 0,
    outputPath: null,
    error: null,
  });

  const clientRef = useRef<CoreClient | null>(null);

  useEffect(() => {
    clientRef.current = new CoreClient();
    return () => {
      clientRef.current?.close();
    };
  }, []);

  const connect = useCallback(async () => {
    setState((s) => ({ ...s, status: 'connecting' }));
    try {
      const client = clientRef.current;
      if (!client) return;
      await client.start(jarPath);
      client.setEventListener((event: CoreEvent) => {
        if (event.type === 'progress') {
          setState((s) => ({
            ...s,
            progress: { platform: event.platform, percentage: event.percentage },
          }));
        } else if (event.type === 'completed') {
          setState((s) => ({
            ...s,
            status: 'completed',
            jobsFound: event.jobsFound,
            outputPath: event.output,
          }));
        } else if (event.type === 'error') {
          setState((s) => ({ ...s, status: 'error', error: event.message }));
        }
      });
      setState((s) => ({ ...s, status: 'idle' }));
    } catch (e) {
      setState((s) => ({
        ...s,
        status: 'error',
        error: `Failed to start Core: ${e instanceof Error ? e.message : String(e)}`,
      }));
    }
  }, [jarPath]);

  const search = useCallback((command: SearchCommand) => {
    setState((s) => ({ ...s, status: 'running', error: null }));
    clientRef.current?.sendCommand(command);
  }, []);

  return {
    ...state,
    connect,
    search,
  };
}
