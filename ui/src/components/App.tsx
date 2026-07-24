import { Box, Text, useInput } from 'ink';
import { useCallback, useEffect, useState } from 'react';
import { useCoreClient } from '../hooks/useCoreClient.ts';
import { Logo } from './Logo.tsx';
import { ProgressBar } from './ProgressBar.tsx';
import { ResultsScreen } from './ResultsScreen.tsx';
import { SearchScreen } from './SearchScreen.tsx';

const ACCENT = '#7C3AED';

interface AppProps {
  jarPath?: string;
}

type Screen = 'search' | 'running' | 'completed' | 'error';

export function App({ jarPath = 'target/JobsTelescope-0.0.1-SNAPSHOT.jar' }: AppProps) {
  const [screen, setScreen] = useState<Screen>('search');
  const { status, progress, jobsFound, outputPath, error, connect, search } =
    useCoreClient(jarPath);

  useEffect(() => {
    connect();
  }, [connect]);

  useEffect(() => {
    if (status === 'running') setScreen('running');
    else if (status === 'completed') setScreen('completed');
    else if (status === 'error') setScreen('error');
    else if (status === 'idle') setScreen('search');
  }, [status]);

  const handleSearch = useCallback(
    (query: string, region: 'BRAZIL' | 'INTERNATIONAL') => {
      search({ command: 'search', query, region });
    },
    [search]
  );

  useInput((_input, key) => {
    if (key.return && (screen === 'completed' || screen === 'error')) {
      setScreen('search');
    }
  });

  const centered = (children: React.ReactNode) => (
    <Box
      flexDirection="column"
      justifyContent="center"
      alignItems="center"
      height="100%"
    >
      {children}
    </Box>
  );

  if (screen === 'search' && status === 'connecting') {
    return centered(
      <>
        <Logo />
        <Text> </Text>
        <Text dimColor>Connecting...</Text>
      </>
    );
  }

  if (screen === 'running') {
    return centered(
      <>
        <Logo />
        <Text> </Text>
        <Text dimColor>Searching</Text>
        <Text> </Text>
        {progress && <ProgressBar platform={progress.platform} percentage={progress.percentage} />}
      </>
    );
  }

  if (screen === 'completed') {
    return centered(
      <ResultsScreen
        jobsFound={jobsFound}
        outputPath={outputPath}
        onNewSearch={() => setScreen('search')}
      />
    );
  }

  if (screen === 'error') {
    return centered(
      <>
        <Logo />
        <Text> </Text>
        <Text color="#EF4444">Error</Text>
        <Text> </Text>
        <Text>{error || 'An unknown error occurred'}</Text>
        <Text> </Text>
        <Text dimColor>Press Enter to go back</Text>
      </>
    );
  }

  return <SearchScreen onSubmit={handleSearch} />;
}
