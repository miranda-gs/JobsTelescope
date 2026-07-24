import { Text } from 'ink';
import { ACCENT } from '../lib/constants.ts';

interface ProgressBarProps {
  platform: string;
  percentage: number;
}

export function ProgressBar({ platform, percentage }: ProgressBarProps) {
  const width = 24;
  const filled = Math.round((percentage / 100) * width);
  const barFilled = '\u2501'.repeat(filled);
  const barEmpty = '\u2501'.repeat(width - filled);

  return (
    <>
      <Text dimColor>{platform}</Text>
      <Text>
        <Text color={ACCENT}>{barFilled}</Text>
        <Text dimColor>{barEmpty}</Text>
        <Text> </Text>
        <Text color={ACCENT}>{percentage}%</Text>
      </Text>
    </>
  );
}
