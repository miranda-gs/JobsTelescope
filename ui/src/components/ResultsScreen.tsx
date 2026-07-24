import { Text } from 'ink';
import { Center } from './Center.tsx';
import { Logo } from './Logo.tsx';

interface ResultsScreenProps {
  jobsFound: number;
  outputPath: string | null;
  onNewSearch: () => void;
}

export function ResultsScreen({ jobsFound, outputPath }: ResultsScreenProps) {
  return (
    <Center>
      <Logo />
      <Text>Completed</Text>
      <Text> </Text>
      <Text>Jobs found  {jobsFound}</Text>
      {outputPath && <Text>Output      {outputPath}</Text>}
      <Text> </Text>
      <Text dimColor>Enter for new search</Text>
    </Center>
  );
}
