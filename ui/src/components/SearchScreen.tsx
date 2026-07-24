import { Box, Text, useInput } from 'ink';
import { useState } from 'react';
import { Logo } from './Logo.tsx';
import { PromptInput } from './PromptInput.tsx';
import { LOGO } from '../lib/logo.ts';

const ACCENT = '#7C3AED';

const LOGO_WIDTH = Math.max(...LOGO.split('\n').map((l) => l.length));

interface SearchScreenProps {
  onSubmit: (query: string, region: 'BRAZIL' | 'INTERNATIONAL') => void;
}

export function SearchScreen({ onSubmit }: SearchScreenProps) {
  const [query, setQuery] = useState('');
  const [step, setStep] = useState<'query' | 'region'>('query');
  const [selectedRegion, setSelectedRegion] = useState<'BRAZIL' | 'INTERNATIONAL'>('BRAZIL');

  useInput((_input, key) => {
    if (step === 'region') {
      if (key.tab) {
        setSelectedRegion((r) => (r === 'BRAZIL' ? 'INTERNATIONAL' : 'BRAZIL'));
      } else if (key.return) {
        onSubmit(query, selectedRegion);
      }
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

  if (step === 'region') {
    const regions: { label: string; value: 'BRAZIL' | 'INTERNATIONAL' }[] = [
      { label: 'Brasil', value: 'BRAZIL' },
      { label: 'Internacional', value: 'INTERNATIONAL' },
    ];

    return centered(
      <>
        <Logo />
        <Text> </Text>
        <Box width={20} flexDirection="column" alignItems="center">
          <Text color={ACCENT}>Região</Text>
          <Box flexDirection="column" alignSelf="flex-start">
            {regions.map((r) => (
              <Text key={r.value} color={selectedRegion === r.value ? ACCENT : undefined}>
                {selectedRegion === r.value ? ' ●' : ' ○'}  {r.label}
              </Text>
            ))}
          </Box>
        </Box>
        <Text> </Text>
        <Text dimColor>Tab para navegar • Enter para confirmar</Text>
      </>
    );
  }

  return centered(
    <>
      <Logo />
      <Text> </Text>
      <Box borderStyle="round" borderColor={ACCENT} width={LOGO_WIDTH}>
        <PromptInput
          placeholder="Digite a vaga desejada"
          onSubmit={(q) => {
            setQuery(q);
            setStep('region');
          }}
        />
      </Box>
      <Text> </Text>
      <Text dimColor>Enter para confirmar</Text>
    </>
  );
}
