export interface Movie {
  movieId: number;
  certificationId?: number;
  certificationCode?: string;
  title: string;
  synopsis?: string;
  runtimeMinutes?: number;
  releaseDate?: string;
  posterUrl?: string;
  trailerUrl?: string;
  movieStatus?: string;
  genres?: string[];
  languages?: string[];
}

export interface Genre {
  genreId: number;
  genreName: string;
}

export interface Language {
  languageId: number;
  movieLanguageId?: number;
  languageName: string;
  languageCode: string;
}

export interface MovieRequest {
  title: string;
  synopsis?: string;
  runtimeMinutes?: number;
  releaseDate?: string;
  posterUrl?: string;
  trailerUrl?: string;
  movieStatus?: string;
  certificationId?: number;
  genreIds?: number[];
  languageIds?: number[];
}
