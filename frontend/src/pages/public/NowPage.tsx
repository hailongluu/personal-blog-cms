import { Helmet } from 'react-helmet-async';
import { MapPin, BookOpen, Code, Coffee } from 'lucide-react';

const nowItems = [
  {
    icon: Code,
    title: 'Building',
    description: 'Working on a personal CMS project with React, Node.js, and PostgreSQL. Exploring serverless architectures and edge computing.',
  },
  {
    icon: BookOpen,
    title: 'Reading',
    description: '"The Pragmatic Programmer" by David Thomas & Andrew Hunt. Re-reading this classic for fresh insights.',
  },
  {
    icon: MapPin,
    title: 'Location',
    description: 'Based in [Your City]. Working remotely and enjoying the flexibility it brings.',
  },
  {
    icon: Coffee,
    title: 'Life',
    description: 'Getting into photography, improving my coffee brewing technique, and trying to run more consistently.',
  },
];

export default function NowPage() {
  return (
    <>
      <Helmet>
        <title>Now — Personal Blog</title>
        <meta name="description" content="What I'm currently focused on — projects, learning, and life." />
      </Helmet>

      <div className="max-w-3xl mx-auto px-4 py-12 md:py-16">
        {/* Header */}
        <div className="mb-12">
          <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">Now</h1>
          <p className="text-lg text-stone-500">
            What I'm currently focused on. Inspired by{' '}
            <a
              href="https://nownownow.com/about"
              target="_blank"
              rel="noopener noreferrer"
              className="text-stone-700 underline hover:text-stone-900"
            >
              Derek Sivers' /now page movement
            </a>
            .
          </p>
          <p className="text-sm text-stone-400 mt-2">Last updated: June 2026</p>
        </div>

        {/* Now items */}
        <div className="grid gap-6 sm:grid-cols-2">
          {nowItems.map((item) => (
            <div
              key={item.title}
              className="bg-white rounded-xl border border-stone-200 p-6 hover:border-stone-300 transition-colors"
            >
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-lg bg-stone-100 flex items-center justify-center">
                  <item.icon size={20} className="text-stone-600" />
                </div>
                <h2 className="font-semibold text-stone-900">{item.title}</h2>
              </div>
              <p className="text-sm text-stone-500 leading-relaxed">{item.description}</p>
            </div>
          ))}
        </div>

        {/* Footer note */}
        <div className="mt-12 p-6 bg-stone-100 rounded-xl">
          <p className="text-sm text-stone-500">
            <strong className="text-stone-700">What's a /now page?</strong> It's a snapshot of what
            I'm focused on at this point in my life. It's updated periodically as priorities change.
          </p>
        </div>
      </div>
    </>
  );
}
