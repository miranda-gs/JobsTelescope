import { Text, useInput } from 'ink';
import { useState } from 'react';

const ACCENT = '#7C3AED';

interface SearchScreenProps {
  onSubmit: (query: string, region: 'BRAZIL' | 'INTERNATIONAL') => void;
}

export function SearchScreen({ onSubmit }: SearchScreenProps) {
  const [query, setQuery] = useState('');
  const [step, setStep] = useState<'query' | 'region'>('query');

  useInput((input, key) => {
    if (step === 'query') {
      if (key.return) {
        setStep('region');
      } else if (key.backspace || key.delete) {
        setQuery((q) => q.slice(0, -1));
      } else {
        setQuery((q) => q + input);
      }
    } else if (step === 'region') {
      if (input === '1') {
        onSubmit(query, 'BRAZIL');
      } else if (input === '2') {
        onSubmit(query, 'INTERNATIONAL');
      }
    }
  });

  if (step === 'query') {
    return (
      <>
        <Text color={ACCENT}>Jobs Telescope</Text>
        <Text> </Text>
        <Text>Search query</Text>
        <Text>{query}_</Text>
        <Text> </Text>
        <Text dimColor>Enter to confirm</Text>
      </>
    );
  }

  return (
    <>
      <Text color={ACCENT}>Jobs Telescope</Text>
      <Text> </Text>
      <Text>Query: {query}</Text>
      <Text> </Text>
      <Text>Region</Text>
      <Text>  1  Brazil</Text>
      <Text>  2  International</Text>
    </>
  );
}
