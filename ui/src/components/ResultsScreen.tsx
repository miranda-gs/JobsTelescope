import { Text } from 'ink';

const ACCENT = '#7C3AED';

interface ResultsScreenProps {
  jobsFound: number;
  outputPath: string | null;
  onNewSearch: () => void;
}

export function ResultsScreen({ jobsFound, outputPath }: ResultsScreenProps) {
  return (
    <>
      <Text color={ACCENT}>Jobs Telescope</Text>
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
