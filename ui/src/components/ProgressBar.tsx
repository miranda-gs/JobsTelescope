import { Text } from 'ink';

interface ProgressBarProps {
  platform: string;
  percentage: number;
}

export function ProgressBar({ platform, percentage }: ProgressBarProps) {
  const width = 30;
  const filled = Math.round((percentage / 100) * width);
  const empty = width - filled;
  const bar = '\u2588'.repeat(filled) + '\u2591'.repeat(empty);

  return (
    <>
      <Text color="cyan">{platform}</Text>
      <Text color="green">
        [{bar}] {percentage}%
      </Text>
    </>
  );
}
