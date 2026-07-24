import { Text } from 'ink';
import { LOGO } from '../lib/logo.ts';

const ACCENT = '#7C3AED';

interface ResultsScreenProps {
  jobsFound: number;
  outputPath: string | null;
  onNewSearch: () => void;
}

export function ResultsScreen({ jobsFound, outputPath }: ResultsScreenProps) {
    return (
      <>
        {LOGO.split('\n').map((line, i) => (
          <Text key={i} color={ACCENT}>{line}</Text>
        ))}
        <Text> </Text>
        <Text>Completed</Text>
      <Text> </Text>
      <Text>Jobs found  {jobsFound}</Text>
      {outputPath && <Text>Output      {outputPath}</Text>}
      <Text> </Text>
      <Text dimColor>Enter for new search</Text>
    </>
  );
}
