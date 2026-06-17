import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({ name: 'linkify', standalone: true })
export class LinkifyPipe implements PipeTransform {

  private static readonly URL_REGEX = /https?:\/\/[^\s<>"')\]]+/g;

  constructor(private readonly sanitizer: DomSanitizer) {}

  transform(text: string | null | undefined): SafeHtml {
    if (!text) return '';
    const escaped = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
    const linked = escaped.replace(LinkifyPipe.URL_REGEX, url =>
      `<a href="${url}" target="_blank" rel="noopener noreferrer" class="chat-link">${url}</a>`
    );
    return this.sanitizer.bypassSecurityTrustHtml(linked);
  }
}
