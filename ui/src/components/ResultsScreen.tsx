import { Text } from 'ink';

interface ResultsScreenProps {
  jobsFound: number;
  outputPath: string | null;
  onNewSearch: () => void;
}

export function ResultsScreen({ jobsFound, outputPath }: ResultsScreenProps) {
  return (
    <>
      <Text bold color="green">
        Search completed!
      </Text>
      <Text> </Text>
      <Text color="cyan">Jobs found: {jobsFound}</Text>
      {outputPath && <Text color="yellow">Output: {outputPath}</Text>}
      <Text> </Text>
      <Text dimColor>Press Enter for a new search</Text>
    </>
  );
}
