import { Text } from 'ink';
import { Logo } from './Logo.tsx';

interface ResultsScreenProps {
  jobsFound: number;
  outputPath: string | null;
  onNewSearch: () => void;
}

export function ResultsScreen({ jobsFound, outputPath }: ResultsScreenProps) {
  return (
    <>
      <Logo />
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
