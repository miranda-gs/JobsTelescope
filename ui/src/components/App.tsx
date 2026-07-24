import { Text, useInput } from 'ink';
import { useCallback, useEffect, useState } from 'react';
import { useCoreClient } from '../hooks/useCoreClient.ts';
import { AnimatedSearching } from './AnimatedSearching.tsx';
import { Center } from './Center.tsx';
import { Logo } from './Logo.tsx';
import { ProgressBar } from './ProgressBar.tsx';
import { ResultsScreen } from './ResultsScreen.tsx';
import { SearchScreen } from './SearchScreen.tsx';
import { ERROR } from '../lib/constants.ts';

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

  if (screen === 'search' && status === 'connecting') {
    return (
      <Center>
        <Logo />
        <Text dimColor>Connecting...</Text>
      </Center>
    );
  }

  if (screen === 'running') {
    return (
      <Center>
        <Logo />
        <AnimatedSearching />
        <Text> </Text>
        {progress && <ProgressBar platform={progress.platform} percentage={progress.percentage} />}
      </Center>
    );
  }

  if (screen === 'completed') {
    return (
      <Center>
        <ResultsScreen
          jobsFound={jobsFound}
          outputPath={outputPath}
          onNewSearch={() => setScreen('search')}
        />
      </Center>
    );
  }

  if (screen === 'error') {
    return (
      <Center>
        <Logo />
        <Text color={ERROR}>Error</Text>
        <Text> </Text>
        <Text>{error || 'An unknown error occurred'}</Text>
        <Text> </Text>
        <Text dimColor>Press Enter to go back</Text>
      </Center>
    );
  }

  return <SearchScreen onSubmit={handleSearch} />;
}
